import json, urllib.request, re
UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"
def get(url, data=None, ref=None):
    h = {"User-Agent": UA}
    if ref: h["Referer"] = ref
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data=data, headers=h), timeout=30) as r:
            return r.status, r.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:
        return str(e)[:60], ""
def flight(h):
    pre = 'self.__next_f.push([1,'
    return ''.join(json.loads(x[len(pre):x.rindex('])')]) for x in re.findall(r'<script>(.*?)</script>', h, re.S) if x.startswith(pre))

print("== ARES")
st, t = get("https://fl-ares.com/api/catalog?page=1"); d = json.loads(t) if st == 200 else {}
print(" catalog", st, len(d.get("items", [])), d.get("hasMore"))
slug = d["items"][0]["slug"] if d.get("items") else "the-eternal-supreme"
st, h = get(f"https://fl-ares.com/manga/{slug}"); s = flight(h) if st == 200 else ""
print(" detail", slug, st, "chapters marker" , '"chapters":[' in s)
m = re.search(r'"number":(\d+(?:\.\d+)?)', s[s.find('"chapters":['):]) if s else None
if m:
    n = m.group(1).removesuffix(".0") if m.group(1).endswith(".0") else m.group(1)
    st, h = get(f"https://fl-ares.com/manga/{slug}/chapter/{n}"); c = flight(h) if st == 200 else ""
    i = c.find('"pages":['); print(" chapter", n, st, "pages", c[i:i+120] if i >= 0 else None)

print("== MANGAWY")
st, t = get("https://mangawy.org/api/browse?sort=popular&page=1"); print(" browse", st, t[:150])
st, t = get("https://mangawy.org/api/filters"); print(" filters", st, len(t))
st, t = get("https://mangawy.org/api/series/search?q=eleceed"); print(" search", st, t[:200])
st, h = get("https://mangawy.org/series/eleceed-orldm5hn"); links = set(re.findall(r'href="(/series/eleceed-orldm5hn/chapter/[^"]+)"', h))
print(" series", st, "chapter links", len(links))
if links:
    l = sorted(links)[0]; st, h = get("https://mangawy.org" + l)
    print(" chapter", l, st, "imgs", len(set(re.findall(r'<img[^>]+src="([^"]*/published/[^"]+)"', h))))
print(" page snippet:", re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", h))[:300])

print("== 3ASQ")
st, h = get("https://3asq.online/?s=&post_type=wp-manga&m_orderby=views"); print(" browse", st, h.count("c-tabs-item__content"))
st, h = get("https://3asq.online/manga/bleach/ajax/chapters/", data=b"", ref="https://3asq.online/manga/bleach/"); print(" chapters", st, h.count("wp-manga-chapter"))
st, h = get("https://3asq.online/manga/bleach/509/"); print(" chapter", st, len(re.findall(r'wp-manga-chapter-img', h)))
