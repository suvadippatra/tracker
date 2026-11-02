import sys
def find_block_end(filename, start_line):
    with open(filename, 'r') as f:
        lines = f.readlines()
        
    stack = 0
    started = False
    for i in range(start_line - 1, len(lines)):
        line = lines[i]
        for char in line:
            if char == '{':
                stack += 1
                started = True
            elif char == '}':
                stack -= 1
                if started and stack == 0:
                    return i + 1
    return -1

print(find_block_end(sys.argv[1], int(sys.argv[2])))
