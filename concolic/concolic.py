import z3
from z3 import If, Or, Extract, Concat, BitVecVal, BitVecSort, RNE, RTZ, fpSignedToFP, fpToSBV, fpFPToFP, fpIsNaN

import subprocess
import sys
import traceback
import itertools
import json

Float = z3.FPSort(8, 24)
Double = z3.FPSort(11, 53)

JAVA_HOME = "/usr/lib/jvm/java-8-openjdk-amd64"
JAVA = "{}/bin/java".format(JAVA_HOME)
PATH_DATA_OUTPUT = "pathConstraints.txt"

class StackEntry:
    def __init__(self, branchId, done, isTrue):
        self.branchId = branchId
        self.done = done
        self.isTrue = isTrue

    def __repr__(self):
        return '({}, {}, {})'.format(self.branchId, self.done, self.isTrue)

class Variable:
    def __init__(self, varType, varName):
        self.varType = varType
        self.varName = varName

class Assignment:
    def __init__(self, leftOp, rightOp):
        self.leftOp = leftOp
        self.rightOp = rightOp

    def __repr__(self):
        return '{} = {}'.format(self.leftOp, self.rightOp)

class PathConstraint:
    def __init__(self, branchId, condition, wasTrue, assignmentIndex):
        self.branchId = branchId
        self.condition = condition
        self.wasTrue = wasTrue
        self.assignmentIndex = assignmentIndex

    def __repr__(self):
        return '({}, {}, {}, {})'.format(self.branchId, self.condition, self.wasTrue, self.assignmentIndex)

class PathData:
    def __init__(self, inputVariables, variables, inputAssignments, assignments, pathConstraints):
        self.inputVariables = inputVariables
        self.variables = variables
        self.inputAssignments = inputAssignments
        self.assignments = assignments
        self.pathConstraints = pathConstraints

class BVInput:
    def __init__(self, val):
        self.val = val

    def __repr__(self):
        return repr(self.val.as_signed_long())

    def inputStr(self):
        return str(self.val.as_signed_long())

class FPInput:
    def __init__(self, val):
        self.val = val

    def __repr__(self):
        return repr(eval(repr(self.val)))

    def inputStr(self):
        val = self.val
        sbits = val.sort().sbits() 
        return str((val.exponent_as_long() << (sbits - 1)) | (val.significand_as_long()))

def readPathData():
    with open(PATH_DATA_OUTPUT, 'r') as f:
        s = f.read()
        lines = s.replace('$', 'D').split('\n')
        i = 0
        inputVariables = []
        while i < len(lines):
            if len(lines[i]) == 0:
                i += 1
                break
            varType, varName = lines[i].split(' ')
            inputVariables.append(Variable(varType, varName))
            i += 1
        variables = []
        while i < len(lines):
            if len(lines[i]) == 0:
                i += 1
                break
            varType, varName = lines[i].split(' ')
            variables.append(Variable(varType, varName))
            i += 1
        inputAssignments = []
        while i < len(lines):
            if len(lines[i]) == 0:
                i += 1
                break
            leftOp, rightOp = lines[i].split(' = ')
            inputAssignments.append(Assignment(leftOp, rightOp))
            i += 1
        assignments = []
        while i < len(lines):
            if len(lines[i]) == 0:
                i += 1
                break
            leftOp, rightOp = lines[i].split(' = ')
            assignments.append(Assignment(leftOp, rightOp))
            i += 1
        pathConstraints = []
        while i < len(lines):
            if len(lines[i]) == 0:
                i += 1
                break
            branchId, condition, wasTrue, assignmentIndex = lines[i].split('; ')
            pathConstraints.append(PathConstraint(int(branchId), condition, wasTrue == 'true', int(assignmentIndex)))
            i += 1
        return PathData(inputVariables, variables, inputAssignments, assignments, pathConstraints)

def makeZ3Var(v):
    t = v.varType
    name = v.varName
    if t == 'BOOLEAN':
        return z3.Bool(name)
    elif t == 'BYTE':
        return z3.BitVec(name, 8)
    elif t == 'SHORT':
        return z3.BitVec(name, 16)
    elif t == 'INT':
        return z3.BitVec(name, 32)
    elif t == 'LONG':
        return z3.BitVec(name, 64)
    elif t == 'FLOAT':
        return z3.FP(name, Float)
    elif t == 'DOUBLE':
        return z3.FP(name, Double)
    elif t == 'CHAR':
        return z3.BitVec(name, 16)
    else:
        raise Exception("unsupported type {}".format(t))

def solveForInputs(sfiStack, sfiPathData):
    z3.set_default_rounding_mode(RNE())

    sfiInputVars = {}
    for sfiV in itertools.chain(sfiPathData.inputVariables, sfiPathData.variables):
        sfiVar = makeZ3Var(sfiV)
        exec('{} = sfiVar'.format(sfiV.varName))
        if sfiV.varName.startswith('INPUT'):
            sfiInputVars[sfiV.varName] = sfiVar

    sfiSolver = z3.Solver()

    sfiPathIndex = 0
    sfiStop = False
    for sfiAssignIndex in range(len(sfiPathData.assignments)):
        sfiPc = sfiPathData.pathConstraints[sfiPathIndex]
        while sfiPc.assignmentIndex == sfiAssignIndex:
            sfiCond = eval(sfiPathData.pathConstraints[sfiPathIndex].condition)
            if not sfiStack[sfiPathIndex].isTrue:
                sfiCond = z3.Not(sfiCond)
            sfiSolver.add(sfiCond)
            sfiPathIndex += 1
            if sfiPathIndex >= len(sfiStack):
                sfiStop = True
                break
            sfiPc = sfiPathData.pathConstraints[sfiPathIndex]
        if sfiStop:
            break
        sfiAssign = sfiPathData.assignments[sfiAssignIndex]
        exec('{} = {}'.format(sfiAssign.leftOp, sfiAssign.rightOp))

    while sfiPathIndex < len(sfiStack):
        sfiPc = sfiPathData.pathConstraints[sfiPathIndex]
        assert sfiPc.assignmentIndex == len(sfiPathData.assignments)
        sfiCond = eval(sfiPathData.pathConstraints[sfiPathIndex].condition)
        if not sfiStack[sfiPathIndex].isTrue:
            sfiCond = z3.Not(sfiCond)
        sfiSolver.add(sfiCond)
        sfiPathIndex += 1

    if sfiSolver.check() == z3.sat:
        m = sfiSolver.model()
        if verbose:
            print('Solved {}'.format(sfiSolver))
        return m, sfiInputVars
    else:
        return None, None

def modelValueToInput(val):
    if val is None:
        return None
    elif type(val) is z3.BitVecNumRef:
        return BVInput(val)
    elif type(val) is z3.FPNumRef:
        return FPInput(val)
    else:
        raise Exception('unsupported model value {} (type {})'.format(val, type(val)))

# returns True if the program crashed, otherwise False
def runInstrumentedProgram(inputs, runCommand):
    env = {
        "JAVA_CONCOLIC_OUTPUT": PATH_DATA_OUTPUT
    }
    for i in range(len(inputs)):
        if inputs[i] is not None:
            env["JAVA_CONCOLIC_INPUT{}".format(i)] = inputs[i].inputStr()
    r = subprocess.run(runCommand, shell=True, env=env, capture_output=True)
    if verbose:
        if len(r.stdout) > 0:
            print(r.stdout.decode('utf-8'))
        if len(r.stderr) > 0:
            print(r.stderr.decode('utf-8'))
    return r.returncode != 0

# load config
with open(sys.argv[1], 'r') as f:
    cfg = json.loads(f.read())
    runCommand = cfg['runCommand']
    stopOnError = cfg['stopOnError']
    verbose = cfg['verbose']

# depth-first exploration of program paths
stack = [] 
inputs = []
while True:
    if inputs is not None:
        inputRepr = 'random inputs' if len(inputs) == 0 else 'inputs {}'.format(repr(inputs))
        if verbose:
            print('Trying {} (path {})'.format(inputRepr, list(map(lambda e: e.isTrue, stack))))
        foundError = runInstrumentedProgram(inputs, runCommand)
        if foundError:
            print('Found error! Inputs: {}'.format(repr(inputs)))
            if stopOnError:
                exit(0)
        pathData = readPathData()
        for i in range(len(pathData.pathConstraints)):
            pc = pathData.pathConstraints[i]
            if i >= len(stack):
                stack.append(StackEntry(pc.branchId, False, pc.wasTrue))
            else:
                entry = stack[i]
                if pc.branchId != entry.branchId or pc.wasTrue != entry.isTrue:
                    raise Exception("program execution did not proceed as expected:\n    expected: {}\n    actual: {}".format(
                        list(map(lambda e: e.isTrue, stack)), 
                        list(map(lambda pc: pc.wasTrue,  pathData.pathConstraints))))
    while len(stack) > 0 and stack[-1].done:
        stack.pop()
    if len(stack) == 0:
        print('Done!')
        break # done
    last = stack[-1]
    last.isTrue = not last.isTrue
    last.done = True
    model, inputVars = solveForInputs(stack, pathData)
    if model is None:
        # infeasible path; continue search without executing program
        inputs = None
    else:
        inputs = []
        for i in range(len(inputVars)):
            inputVar = inputVars['INPUT{}'.format(i)]
            inputs.append(modelValueToInput(model[inputVar]))
