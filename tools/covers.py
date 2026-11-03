import json, urllib.request, collections, sys
UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"
def get(url, headers=None):
    req = urllib.request.Request(url, headers={"User-Agent": UA, **(headers or {})})
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, r.headers.get("content-type", ""), r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers.get("content-type", ""), b""
    except Exception as e:
        return str(e)[:60], "", b""
items = []
page = 1
while True:
    st, ct, body = get(f"https://fl-ares.com/api/catalog?page={page}")
    d = json.loads(body)
    items += d["items"]
    if not d.get("hasMore") or page > 200: break
    page += 1
print("total items", len(items), "pages", page)
hosts = collections.Counter()
bad = []
for i in items:
    p = i.get("poster")
    if not p:
        bad.append((i["slug"], "NO POSTER", "")); continue
    host = p.split("/")[2]
    hosts[host] += 1
    if host != "media.fl-ares.com":
        a = get(p)
        b = get(p, {"Referer": "https://fl-ares.com/"})
        bad.append((i["slug"], p, f"noref={a[0]} {a[1]} {len(a[2])} | ref={b[0]} {b[1]} {len(b[2])}"))
print(hosts)
# sample media covers
import random
media = [i["poster"] for i in items if i.get("poster", "") and "media.fl-ares.com" in i["poster"]]
for p in random.sample(media, min(15, len(media))):
    a = get(p); b = get(p, {"Referer": "https://fl-ares.com/"})
    print("media", a[0], a[1], len(a[2]), "| ref", b[0], len(b[2]), p[-40:])
for x in bad: print("EXT", *x)
