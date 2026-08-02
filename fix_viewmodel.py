with open('app/src/main/java/com/example/ui/viewmodels/StoreViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if i == 100 or i == 101: # 0-indexed, so lines 101 and 102
        # these are the extra braces
        continue
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/viewmodels/StoreViewModel.kt', 'w') as f:
    f.writelines(new_lines)
