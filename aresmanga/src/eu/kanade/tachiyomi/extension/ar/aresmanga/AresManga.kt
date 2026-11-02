package eu.kanade.tachiyomi.extension.ar.aresmanga

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import keiyoushi.annotation.Source

@Source
abstract class AresManga : MangaThemesia() {
    override val mangaUrlDirectory = "/series"
}
