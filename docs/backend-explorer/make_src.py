"""Embeds the extracted data into the template -> backend-explorer.src.html"""
import json, os
d = os.path.dirname(os.path.abspath(__file__))
t = open(os.path.join(d, 'explorer.tmpl.html'), encoding='utf8').read()
classes = json.load(open(os.path.join(d, 'explorer-data.json')))['classes']
cats = json.load(open(os.path.join(d, 'annotations-data.json')))
dump = lambda o: json.dumps(o, ensure_ascii=False, separators=(',', ':')).replace('</', '<\\/').replace('{{', '{ {').replace('}}', '} }')
t = t.replace('/*__CLASSES__*/[]', dump(classes)).replace('/*__CATS__*/[]', dump(cats))
open(os.path.join(d, 'backend-explorer.src.html'), 'w', encoding='utf8').write(t)
print(len(t), 'bytes')
