import json, urllib.request, re, random
UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"
def get(url, ref=True):
    h = {"User-Agent": UA}
    if ref: h["Referer"] = "https://fl-ares.com/"
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers=h), timeout=20) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, b""
    except Exception as e:
        return str(e)[:40], b""
items = []
for page in range(1, 101):
    d = json.loads(get(f"https://fl-ares.com/api/catalog?page={page}")[1])
    items += d["items"]
    if not d.get("hasMore"): break
media = [i for i in items if "media.fl-ares.com" in (i.get("poster") or "")]
random.seed(1)
sample = random.sample(media, 120)
dead = []
for i in sample:
    st, _ = get(i["poster"])
    if st != 200: dead.append(i)
print("sampled", len(sample), "dead", len(dead))
for i in dead[:6]:
    st, html = get("https://fl-ares.com/manga/" + i["slug"])
    imgs = re.findall(r'<img[^>]*fetchPriority="high"[^>]*src="([^"]+)"', html.decode("utf-8", "ignore"))
    print(i["slug"], "| list:", i["poster"][-50:], "| page imgs:", [x[-60:] for x in imgs[:3]])
    for x in imgs[:2]:
        print("   ", get(x)[0], x[-60:])
