import json, urllib.request
q = "query($q:String){Media(search:$q,type:MANGA){coverImage{large}}}"
for t in ["The Eternal Supreme", "Emperor Xianwu", "Shadow Queen", "The Ultimate of All Ages"]:
    req = urllib.request.Request("https://graphql.anilist.co", data=json.dumps({"query": q, "variables": {"q": t}}).encode(),
                                 headers={"Content-Type": "application/json", "Accept": "application/json", "User-Agent": "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/130.0 Mobile Safari/537.36"})
    try:
        print(t, "->", json.load(urllib.request.urlopen(req, timeout=20))["data"]["Media"]["coverImage"]["large"])
    except Exception as e:
        print(t, "ERR", e)
