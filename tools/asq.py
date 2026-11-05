import urllib.request, urllib.parse, re
UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"
def req(url, data=None, extra=None):
    h = {"User-Agent": UA, "Referer": "https://3asq.online/manga/bleach/", "X-Requested-With": "XMLHttpRequest"}
    h.update(extra or {})
    body = urllib.parse.urlencode(data).encode() if data is not None else None
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data=body, headers=h), timeout=30) as r:
            return r.status, r.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")[:300]
def show(name, r):
    st, t = r
    lis = re.findall(r'<li class="wp-manga-chapter', t)
    links = re.findall(r'href="(https://3asq\.online/manga/bleach/[^"]+)"', t)
    print(f"== {name}: {st} len={len(t)} li={len(lis)} links={len(set(links))} first={links[:2]}")
    print("   pagination:", re.findall(r'(data-page="\d+"|class="[^"]*pagination[^"]*"|t=\d+)', t)[:6])
    print("   dates:", re.findall(r'chapter-release-date">\s*(?:<[^>]+>\s*)*([^<]{0,40})', t)[:3])
    print("   snippet:", re.sub(r"\s+", " ", t[:300]))
show("ajax/chapters", req("https://3asq.online/manga/bleach/ajax/chapters/", data={}))
show("ajax/chapters?t=2", req("https://3asq.online/manga/bleach/ajax/chapters/?t=2", data={}))
show("admin-ajax", req("https://3asq.online/wp-admin/admin-ajax.php", data={"action": "manga_get_chapters", "manga": "1903"}))
show("admin-ajax paged", req("https://3asq.online/wp-admin/admin-ajax.php", data={"action": "manga_get_chapters", "manga": "1903", "paged": "2"}))
st, t = req("https://3asq.online/manga/bleach/509/")
imgs = re.findall(r'<img[^>]+(?:data-src|src)="\s*([^"]+)"[^>]*class="wp-manga-chapter-img', t) or re.findall(r'class="wp-manga-chapter-img[^"]*"[^>]*(?:data-src|src)="\s*([^"]+)"', t)
print("== chapter page", st, len(t), "imgs", len(imgs), imgs[:2])
print("   reading-content:", "reading-content" in t, re.findall(r'<img[^>]*>', t[t.find("reading-content"):t.find("reading-content")+3000])[:2])
