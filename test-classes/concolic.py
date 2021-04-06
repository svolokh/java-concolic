import z3
import subprocess
import sys
import traceback

JAVA_HOME = "/usr/lib/jvm/java-8-openjdk-amd64"
JAVA = "{}/bin/java".format(JAVA_HOME)
INSTRUMENTATION_CLASSES = "../instrumentation/bin"
CLASSES_DIR = "sootOutput"
ENTRYPOINT_CLASS = "MyClass"
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

class PathConstraint:
    def __init__(self, branchId, condition, wasTrue):
        self.branchId = branchId
        self.condition = condition
        self.wasTrue = wasTrue

class PathData:
    def __init__(self, variables, assignments, pathConstraints):
        self.variables = variables
        self.assignments = assignments
        self.pathConstraints = pathConstraints

def readPathData():
    with open(PATH_DATA_OUTPUT, 'r') as f:
        s = f.read()
        lines = s.replace('$', 'D').split('\n')
        i = 0
        variables = []
        while i < len(lines):
            if len(lines[i]) == 0:
                i += 1
                break
            varType, varName = lines[i].split(' ')
            variables.append(Variable(varType, varName))
            i += 1
        assignments = []
        while i < len(lines):
            if len(lines[i]) == 0:
                i += 1
                break
            assignments.append(lines[i])
            i += 1
        pathConstraints = []
        while i < len(lines):
            if len(lines[i]) == 0:
                i += 1
                break
            branchId, condition, wasTrue = lines[i].split('; ')
            pathConstraints.append(PathConstraint(int(branchId), condition, wasTrue == 'true'))
            i += 1
        return PathData(variables, assignments, pathConstraints)

def makeZ3Var(v):
    t = v.varType
    name = v.varName
    if t == 'BYTE':
        return z3.BitVec(name, 8)
    elif t == 'SHORT':
        return z3.BitVec(name, 16)
    elif t == 'INT':
        return z3.BitVec(name, 32)
    elif t == 'LONG':
        return z3.BitVec(name, 64)
    elif t == 'FLOAT':
        return z3.FP(name, z3.FPSort(8, 23))
    elif t == 'DOUBLE':
        return z3.FP(name, z3.FPSort(11, 52))
    elif t == 'CHAR':
        return z3.BitVec(name, 16)
    else:
        raise Exception("unsupported type {}".format(t))

def solveForInputs(sfiStack, sfiPathData):
    sfiInputVars = {}
    for sfiV in sfiPathData.variables:
        sfiVar = makeZ3Var(sfiV)
        exec('{} = sfiVar'.format(sfiV.varName))
        if sfiV.varName.startswith('INPUT'):
            sfiInputVars[sfiV.varName] = sfiVar
    for sfiAssign in sfiPathData.assignments:
        exec(sfiAssign)
    sfiCounter = 0
    sfiSolver = z3.Solver()
    for sfiPc in sfiPathData.pathConstraints:
        if sfiCounter >= len(sfiStack):
            break
        sfiCond = eval(sfiPc.condition)
        if not sfiStack[sfiCounter].isTrue:
            sfiCond = z3.Not(sfiCond)
        sfiSolver.add(sfiCond)
        sfiCounter += 1
    if sfiSolver.check() == z3.sat:
        return sfiSolver.model(), sfiInputVars
    else:
        return None, None

def modelValueToInput(val):
    if type(val) is z3.BitVecNumRef:
        return val.as_signed_long()
    else:
        raise Exception('unsupported model value {}'.format(val))

# returns True if the program halted successfully, False if an error occurred
def runInstrumentedProgram(inputs):
    env = {
        "JAVA_CONCOLIC_OUTPUT": PATH_DATA_OUTPUT
    }
    for i in range(len(inputs)):
        env["JAVA_CONCOLIC_INPUT{}".format(i)] = str(inputs[i])
    r = subprocess.run([JAVA, "-cp", "{}:{}".format(CLASSES_DIR, INSTRUMENTATION_CLASSES), ENTRYPOINT_CLASS], env=env, capture_output=True)
    if len(r.stdout) > 0:
        print(r.stdout.decode('utf-8'))
    if len(r.stderr) > 0:
        print(r.stderr.decode('utf-8'))
    return r.returncode != 0

# depth-first exploration of program paths
stack = [] 
inputs = []
while True:
    inputRepr = 'random inputs' if len(inputs) == 0 else 'inputs {}'.format(inputs)
    print('Trying {}'.format(inputRepr))
    foundError = runInstrumentedProgram(inputs)
    if foundError:
        print('Found error!')
    pathData = readPathData()
    for i in range(len(pathData.pathConstraints)):
        pc = pathData.pathConstraints[i]
        if i >= len(stack):
            stack.append(StackEntry(pc.branchId, False, pc.wasTrue))
        else:
            entry = stack[i]
            if pc.branchId != entry.branchId or pc.wasTrue != entry.isTrue:
                raise Exception("program execution did not proceed as expected")
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
        raise Exception('no solution to path')
    inputs = []
    for i in range(len(inputVars)):
        inputVar = inputVars['INPUT{}'.format(i)]
        inputs.append(modelValueToInput(model[inputVar]))
