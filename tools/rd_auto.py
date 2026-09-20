# -*- coding: utf-8 -*-
"""
命令行版分析（不依赖 GUI 交互）：
  定位 AR 的加速实体绘制（顶点属性含 iris_Entity / at_tangent），
  给出其 output / depth，判断是否写进了 Iris 的 gbuffer。

运行：
  qrenderdoc.exe --python tools/rd_auto.py <capture.rdc>
结果写入 tools/rd_auto_out.txt
"""

import os
import sys
import traceback

import renderdoc as rd

OUT = r'D:/Programs/MC/26.1/AcceleratedRendering-Ported/tools/rd_auto_out.txt'

MAX_SAMPLE = 40
# 是否全量扫描（找 iris_Entity 这类稀有属性）
SCAN_ALL = os.environ.get('RD_SCAN_ALL') == '1'

lines = []


def p(s=''):
    lines.append(str(s))
    print(s)
    sys.stdout.flush()


def main():
    # capture 路径：优先取命令行参数，其次环境变量
    cap_path = None
    for a in sys.argv:
        if a.lower().endswith('.rdc'):
            cap_path = a
            break

    if cap_path is None:
        cap_path = os.environ.get('RD_CAPTURE')

    if cap_path is None:
        p('!! 未提供 .rdc 路径')
        return

    p('capture: %s' % cap_path)

    cap = rd.OpenCaptureFile()
    r = cap.OpenFile(cap_path, '', None)
    p('OpenFile -> %s' % r)

    _, ctrl = cap.OpenCapture(rd.ReplayOptions(), None)
    if ctrl is None:
        p('!! OpenCapture 失败')
        return

    try:
        # 纹理尺寸表
        tex = {}
        for t in ctrl.GetTextures():
            try:
                rid = int(str(t.resourceId).split('::')[-1])
            except Exception:
                continue
            tex[rid] = '%dx%d %s' % (t.width, t.height, t.format.Name())

        root = ctrl.GetRootActions()
        draws = []

        def walk(a):
            if 'Drawcall' in str(a.flags):
                draws.append(a)
            for c in (a.children or []):
                walk(c)

        for a in root:
            walk(a)

        p('draw 总数: %d（采样 %d）' % (len(draws), MAX_SAMPLE))

        agg = {}
        if SCAN_ALL:
            targets = draws
            p('!! 全量扫描 %d 个 draw（可能较慢）' % len(targets))
        else:
            step = max(1, len(draws) // MAX_SAMPLE)
            targets = draws[::step][:MAX_SAMPLE]

        for a in targets:
            try:
                ctrl.SetFrameEvent(a.eventId, True)
                gen = ctrl.GetPipelineState()

                names = tuple(sorted(set(x.name for x in gen.GetVertexInputs() if x.name)))

                outs = gen.GetOutputTargets()
                oid = 0
                if outs:
                    try:
                        oid = int(str(outs[0].resource).split('::')[-1])
                    except Exception:
                        pass

                did = 0
                try:
                    did = int(str(gen.GetDepthTarget().resource).split('::')[-1])
                except Exception:
                    pass

                vs = str(gen.GetShader(rd.ShaderStage.Vertex))

                key = names
                g = agg.setdefault(key, {'n': 0, 'idx': [], 'outs': set(), 'vs': set()})
                g['n'] += 1
                g['idx'].append(a.numIndices)
                g['outs'].add((oid, did))
                g['vs'].add(vs)
            except Exception:
                continue

        p('')
        p('=' * 70)
        p('按顶点属性名聚合')
        p('=' * 70)
        for names, g in sorted(agg.items(), key=lambda kv: -kv[1]['n']):
            idxs = [i for i in g['idx'] if i is not None]
            p('')
            p('  属性(%d): %s' % (len(names), list(names)))
            p('    draw 数=%d  idx=%s' % (g['n'], sorted(set(idxs))[:8]))
            p('    VS: %s' % sorted(g['vs']))
            for (oid, did) in sorted(g['outs']):
                p('    output=[%d] %-24s depth=[%d] %s'
                  % (oid, tex.get(oid, '?'), did, tex.get(did, '?')))

        p('')
        p('=' * 70)
        p('AR 加速实体（含 iris_Entity 或 at_tangent）')
        p('=' * 70)
        found = False
        for names, g in agg.items():
            if 'iris_Entity' in names or 'at_tangent' in names:
                found = True
                idxs = [i for i in g['idx'] if i is not None]
                p('  属性: %s' % list(names))
                p('    n=%d  idx=%s  VS=%s' % (g['n'], sorted(set(idxs))[:8], sorted(g['vs'])))
                for (oid, did) in sorted(g['outs']):
                    p('    >>> output=[%d] %s   depth=[%d] %s'
                      % (oid, tex.get(oid, '?'), did, tex.get(did, '?')))
        if not found:
            p('  (未找到)')

    except Exception:
        p(traceback.format_exc())
    finally:
        try:
            ctrl.Shutdown()
            cap.Shutdown()
        except Exception:
            pass


main()

with open(OUT, 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))

print('')
print('### 已写出: %s' % OUT)

# 命令行模式（--python）下跑完即退出，否则 qrenderdoc 会停在 GUI 不返回
sys.exit(0)
