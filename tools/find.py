import json, urllib.request, urllib.parse, re
UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"
def get(url):
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=30) as r:
            return r.status, r.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:
        return str(e)[:50], ""
queries = ["Return of the First Generation", "First Generation", "Descendant 1000 Years", "1000 Years Later", "Reincarnates as His Descendant", "عودة المؤسس", "المؤسس الأول", "Eldragon", "إلدراغون"]
for q in queries:
    e = urllib.parse.quote(q)
    st, t = get(f"https://fl-ares.com/api/catalog?q={e}")
    ares = [(i["title"], i["slug"], i["_count"]["chapters"]) for i in json.loads(t)["items"]] if st == 200 else st
    st, t = get(f"https://3asq.online/?s={e}&post_type=wp-manga")
    asq = re.findall(r'<div class="post-title">\s*<h3 class="h4"><a href="https://3asq\.online/manga/([^"/]+)/">([^<]+)</a>', t) if st == 200 else st
    st, t = get(f"https://mangawy.org/api/series/search?q={e}")
    wy = [(i.get("title"), i.get("slug")) for i in json.loads(t)] if st == 200 else st
    print(f"[{q}]\n  ares: {ares}\n  3asq: {asq}\n  mangawy: {wy}")
