"""Reads the backend's Java sources and writes explorer-data.json.
Run:  python3 extract.py  (from this folder)"""
import re, os, json, sys
SRC = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../backend/student-manager/src/main/java/com/student_manager'))

def anns_in(text):
    """annotations (name, args) at paren depth 0 in text"""
    out, i, n = [], 0, len(text)
    while i < n:
        if text[i] == '@' and i+1 < n and text[i+1].isalpha():
            j = i+1
            while j < n and (text[j].isalnum() or text[j] in '._'): j += 1
            name = text[i+1:j]; args = ''
            k = j
            while k < n and text[k] in ' \t\n': k += 1
            if k < n and text[k] == '(':
                d, m = 0, k
                while m < n:
                    if text[m] == '(': d += 1
                    elif text[m] == ')':
                        d -= 1
                        if d == 0: break
                    elif text[m] == '"':
                        m += 1
                        while text[m] != '"':
                            m += 2 if text[m] == '\\' else 1
                    m += 1
                args = text[k+1:m]; j = m+1
            if name != 'interface': out.append((name.split('.')[-1], re.sub(r'\s+', ' ', args).strip()))
            i = j
        else: i += 1
    return out

def first_sentence(doc):
    if not doc: return ''
    t = re.sub(r'^\s*\*+ ?', '', doc, flags=re.M)
    t = re.sub(r'\{@(?:code|link|linkplain)\s+([^}]*)\}', lambda m: m.group(1).split()[-1] if 'link' in m.group(0)[:9] else m.group(1), t)
    t = re.sub(r'<[^>]+>', ' ', t)
    paras = re.split(r'\n\s*\n|@param|@return|@throws', t)[0]
    t = re.sub(r'\s+', ' ', paras).strip()
    m = re.match(r'(.+?[.!?])(\s|$)', t)
    return (m.group(1) if m else t)[:420]

def members(body):
    """split class body (depth 1) into (doc, header_text) members; header excludes the {} body"""
    res, i, n = [], 0, len(body)
    doc, buf, depth_p = None, '', 0
    while i < n:
        c = body[i]
        if body.startswith('/**', i):
            e = body.index('*/', i); doc = body[i+3:e]; i = e+2; continue
        if body.startswith('/*', i):
            i = body.index('*/', i)+2; continue
        if body.startswith('//', i):
            e = body.find('\n', i); i = n if e < 0 else e; continue
        if c == '"':
            if body.startswith('"""', i):
                e = body.index('"""', i+3)+3
            else:
                e = i+1
                while body[e] != '"': e += 2 if body[e] == '\\' else 1
                e += 1
            buf += body[i:e]; i = e; continue
        if c == '(' : depth_p += 1
        if c == ')' : depth_p -= 1
        if c == '{' and depth_p == 0:
            d, j = 0, i
            while j < n:
                if body[j] == '{': d += 1
                elif body[j] == '}':
                    d -= 1
                    if d == 0: break
                elif body[j] == '"':
                    j += 1
                    while body[j] != '"': j += 2 if body[j] == '\\' else 1
                j += 1
            res.append((doc, buf.strip(), True)); doc, buf = None, ''; i = j+1; continue
        if c == ';' and depth_p == 0:
            res.append((doc, buf.strip(), False)); doc, buf = None, ''; i += 1; continue
        buf += c; i += 1
    return res

def strip_anns(text):
    out, i, n = '', 0, len(text)
    while i < n:
        if text[i] == '@' and i+1 < n and text[i+1].isalpha():
            j = i+1
            while j < n and (text[j].isalnum() or text[j] in '._'): j += 1
            k = j
            while k < n and text[k] in ' \t\n': k += 1
            if k < n and text[k] == '(':
                d, m = 0, k
                while m < n:
                    if text[m] == '(': d += 1
                    elif text[m] == ')':
                        d -= 1
                        if d == 0: break
                    elif text[m] == '"':
                        m += 1
                        while text[m] != '"': m += 2 if text[m] == '\\' else 1
                    m += 1
                j = m+1
            i = j
        else: out += text[i]; i += 1
    return re.sub(r'\s+', ' ', out).strip()

def parse(path):
    t = open(path, encoding='utf8').read()
    m = re.search(r'^(?:public\s+)?(?:abstract\s+|final\s+)?(class|interface|enum|record)\s+(\w+)', t, re.M)
    if not m: return None
    kind, name = m.group(1), m.group(2)
    # annotations + javadoc before declaration
    head = t[:m.start()]
    jd = list(re.finditer(r'/\*\*(.*?)\*/', head, re.S))
    cutoff = 0
    for mm in re.finditer(r'^import .*?;\s*$|^package .*?;\s*$', head, re.M): cutoff = mm.end()
    pre = head[cutoff:]
    doc = ''
    if jd and jd[-1].start() >= cutoff: doc = jd[-1].group(1); pre = pre.replace(jd[-1].group(0), '')
    pre = re.sub(r'//[^\n]*', '', pre); pre = re.sub(r'/\*.*?\*/', '', pre, flags=re.S)
    anns = anns_in(pre)
    decl_end = t.index('{', m.end())
    decl = re.sub(r'\s+', ' ', t[m.start():decl_end])
    ext = re.search(r'extends\s+([\w<>, ?]+?)(?:\s+implements|\s*$)', decl)
    impl = re.search(r'implements\s+([\w<>, ?]+)', decl)
    # class body
    d, j = 0, decl_end
    while True:
        if t[j] == '{': d += 1
        elif t[j] == '}':
            d -= 1
            if d == 0: break
        elif t[j] == '"':
            if t.startswith('"""', j): j = t.index('"""', j+3)+2
            else:
                j += 1
                while t[j] != '"': j += 2 if t[j] == '\\' else 1
        elif t.startswith('//', j): j = t.index('\n', j)
        elif t.startswith('/*', j): j = t.index('*/', j)+1
        j += 1
    body = t[decl_end+1:j]
    mem = []
    for mdoc, header, has_body in members(body):
        if not header: continue
        a = anns_in(header); h = strip_anns(header)
        if kind == 'enum' and not mem and not has_body and ('(' not in h or True):
            pass
        mem.append({'ann': a, 'sig': h, 'doc': first_sentence(mdoc), 'body': has_body})
    return {'name': name, 'kind': kind, 'ann': anns, 'doc': first_sentence(doc), 'extends': ext.group(1).strip() if ext else '',
            'implements': impl.group(1).strip() if impl else '', 'members': mem, 'path': os.path.relpath(path, SRC)}

classes = {}
for r, _, fs in os.walk(SRC):
    for f in sorted(fs):
        if f.endswith('.java'):
            c = parse(os.path.join(r, f))
            if c: classes[c['name']] = c

def layer(c):
    n, p = c['name'], c['path']
    A = [a for a, _ in c['ann']]
    if n.endswith('Controller'): return 'web'
    if n.endswith('ServiceImpl') or n == 'CrudServiceSupport': return 'service'
    if n.endswith('Repository') and c['kind'] == 'interface': return 'data'
    if 'Entity' in A or 'MappedSuperclass' in A: return 'domain'
    if re.search(r'(Create|Update|Login|Register|ChangePassword)\w*Request$', n): return 'request'
    if p.startswith('shared/') or n in ('SessionLogin', 'AccountProvisioner'): return 'cross'
    return None

def short_anns(c): return [a for a, _ in c['ann']]

def clean_type(s): return re.sub(r'\s+', ' ', s)

out = {'classes': []}
for n, c in classes.items():
    L = layer(c)
    if not L: continue
    o = {'id': n, 'layer': L, 'kind': c['kind'], 'path': 'com/student_manager/' + c['path'], 'ann': c['ann'], 'doc': c['doc'],
         'extends': c['extends'], 'implements': c['implements'], 'deps': [], 'endpoints': [], 'methods': [], 'fields': []}
    base = ''
    for a, args in c['ann']:
        if a == 'RequestMapping': base = (re.findall(r'"([^"]*)"', args) or [''])[0]
    for m in c['members']:
        sig, ann = m['sig'], m['ann']
        names = [a for a, _ in ann]
        if c['kind'] == 'enum': continue
        # constructor / initialisers skipped
        if re.search(r'\b(?:private|protected)?\s*final\s+[\w<>, ?\[\]]+\s+\w+$', sig) and not m['body'] and '(' not in sig:
            ty, nm = re.match(r'.*?final\s+(.+)\s+(\w+)$', sig).groups()
            o['deps'].append(ty.replace('private ', '').strip()); continue
        if '(' not in sig or sig.endswith('=') or ' = ' in sig.split('(')[0]:
            fm = re.match(r'(?:(?:private|protected|public|static|final|transient)\s+)*([\w<>\[\], ?.]+?)\s+(\w+)(?:\s*=.*)?$', sig)
            if fm and c['kind'] != 'interface':
                o['fields'].append({'name': fm.group(2), 'type': fm.group(1), 'ann': [(a, g) for a, g in ann]})
            continue
        mm = re.match(r'(?:(?:public|private|protected|static|default|abstract|final|synchronized)\s+)*(?:<[^>]+>\s+)?([\w<>\[\], ?.]+?)\s+(\w+)\s*\((.*)\)(?:\s+throws\s+[\w, .]+)?$', sig)
        if not mm: continue
        ret, nm, params = mm.groups()
        params = re.sub(r'\s+', ' ', params)
        if L == 'web':
            mapping = next(((a, g) for a, g in ann if a.endswith('Mapping')), None)
            if mapping:
                verb = mapping[0][:-7].upper()
                sub = (re.findall(r'"([^"]*)"', mapping[1]) or [''])[0]
                path = (base + ('/' + sub.lstrip('/') if sub else ''))
                pre = next((g for a, g in ann if a == 'PreAuthorize'), '')
                o['endpoints'].append({'verb': verb, 'path': path, 'name': nm, 'ret': ret, 'params': params, 'pre': re.sub(r'^"|"$', '', pre), 'doc': m['doc']})
        else:
            if nm == 'main' or {'private','protected'} & set(sig.split('(')[0].split()): continue
            o['methods'].append({'name': nm, 'ret': ret, 'params': params, 'ann': ann, 'doc': m['doc']})
    out['classes'].append(o)
order = {'web': 0, 'service': 1, 'data': 2, 'domain': 3, 'request': 4, 'cross': 5}
out['classes'].sort(key=lambda o: (order[o['layer']], o['id']))
json.dump(out, open(os.path.join(os.path.dirname(__file__), 'explorer-data.json'), 'w'), indent=1)
print(len(out['classes']), 'classes;', sum(len(c['endpoints']) for c in out['classes']), 'endpoints;', sum(len(c['fields']) for c in out['classes']), 'fields;', sum(len(c['methods']) for c in out['classes']), 'methods')
from collections import Counter; print(Counter(c['layer'] for c in out['classes']))
