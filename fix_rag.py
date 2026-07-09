import re
data = open('app/src/main/kotlin/com/aichat/app/engine/rag/RagEngineImpl.kt', 'rb').read()
# Remove BOM
if data[:3] == b'\xef\xbb\xbf':
    data = data[3:]
    print('Removed BOM')
# Normalize line endings to LF
data = data.replace(b'\r\n', b'\n')
# Remove trailing blank lines
data = data.rstrip(b'\n') + b'\n'
open('app/src/main/kotlin/com/aichat/app/engine/rag/RagEngineImpl.kt', 'wb').write(data)
lines = data.decode('utf-8').split('\n')
print(f'Lines: {len(lines)}')
print(f'First 4 bytes: {data[:4].hex()}')
print('Done')
