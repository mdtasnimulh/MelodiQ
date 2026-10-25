package com.tasnimulhasan.radio.domain

/**
 * Built-in stations, all https so Android's cleartext block does not stop them. Stream addresses
 * change without notice, so any of these can stop working; users can add their own and save them
 * under any group (see SavedStationStore).
 *
 * The Bangladeshi group has no built-in entries: no current, verified https stream addresses were
 * available when this was written. Saved stations the user puts in that group appear there.
 */

object RadioStations {
    val defaults = listOf(
        // Bangladeshi Station URL's
        RadioStation("Bangladesh Betar", "National radio, news and Bangla programs", "https://stream.zeno.fm/8sn7cac604zuv", StationGroup.BANGLADESHI),
        RadioStation("Radio Foorti", "Bangladeshi music and entertainment", "https://radiofoorti.fm/api/stream", StationGroup.BANGLADESHI),
        RadioStation("Dhaka FM 90.4", "Bangladeshi music and entertainment", "https://play.dhakafm904.com/;", StationGroup.BANGLADESHI),
        RadioStation("Radio Today", "Bangladeshi music, news and talk", "https://stream-285.surfernetwork.com/8wv4d8g4344tv", StationGroup.BANGLADESHI),
        RadioStation("Radio Bhumi", "Bangla music and entertainment", "https://stream-156.zeno.fm/be5bqmk1k18uv", StationGroup.BANGLADESHI),
        RadioStation("Rajshahi FM Bangla", "Bangla music and regional programs", "https://stream.zeno.fm/nxfshdayjrdvv", StationGroup.BANGLADESHI),
        RadioStation("Radio GoonGoon", "Bangla music and entertainment", "https://audio.streamcast.xyz/listen/radiogoongoon/radio.mp3", StationGroup.BANGLADESHI),
        RadioStation("Mellow Bangla", "Bangla music and relaxing songs", "https://radio.mellowbangla.com/stream", StationGroup.BANGLADESHI),
        RadioStation("A2Z Radio", "Bangladeshi music and entertainment", "https://listen.radioking.com/radio/1743/stream/125", StationGroup.BANGLADESHI),
        RadioStation("Radio Vivid Voice", "Pop, rock and talk radio", "https://stream.zeno.fm/xuin6ocdc2juv", StationGroup.BANGLADESHI),

        // News Stations URL's
        RadioStation("BBC World Service", "International news and analysis", "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service", StationGroup.NEWS),
        RadioStation("NPR News", "US and international news", "https://npr-ice.streamguys1.com/live.mp3", StationGroup.NEWS),
        RadioStation("KQED Public Radio", "Public radio news and analysis", "https://streams.kqed.org/kqedradio", StationGroup.NEWS),
        RadioStation("Al Jazeera English", "Global news and current affairs", "https://live-hls-audio-web-aje.getaj.net/VOI/JE/01.m3u8", StationGroup.NEWS),
        RadioStation("France Culture", "French culture, news and discussion", "https://icecast.radiofrance.fr/franceculture-midfi.mp3", StationGroup.NEWS),
        RadioStation("Times Radio", "UK news, interviews and analysis", "https://timesradio.wireless.radio/stream", StationGroup.NEWS),
        RadioStation("WNYC FM", "New York news and public affairs", "https://fm939.wnyc.org/wnycfm", StationGroup.NEWS),
        RadioStation("KPBS Radio", "San Diego public radio and news", "https://kpbs.streamguys1.com/kpbs-mp3", StationGroup.NEWS),
        RadioStation("WHYY FM", "Philadelphia news and public radio", "https://whyy.streamguys1.com/whyy-mp3", StationGroup.NEWS),
        RadioStation("WBEZ Chicago", "Chicago public radio and news", "https://stream.wbez.org/wbez128.mp3", StationGroup.NEWS),
        RadioStation("The Current Minnesota", "Music, culture and public radio", "https://current.stream.publicradio.org/current.mp3", StationGroup.NEWS),
        RadioStation("Deutschlandfunk Nova", "German news, science and culture", "https://st03.sslstream.dlf.de/dlf/03/128/mp3/stream.mp3", StationGroup.NEWS),
        RadioStation("RTÉ Radio 1", "Irish news, discussion and current affairs", "https://icecast.rte.ie/radio1", StationGroup.NEWS),
        RadioStation("ORF Ö1", "Austrian news, culture and current affairs", "https://orf-live.ors-shoutcast.at/oe1-q2a", StationGroup.NEWS),
        RadioStation("NRK Alltid Nyheter", "Norwegian rolling news", "https://lyd.nrk.no/nrk_radio_alltid_nyheter_mp3_h", StationGroup.NEWS),

        // Music Station URL's
        RadioStation("SomaFM Groove Salad", "Ambient / downtempo", "https://ice1.somafm.com/groovesalad-128-mp3", StationGroup.MUSIC),
        RadioStation("SomaFM Defcon", "Electronic", "https://ice1.somafm.com/defcon-128-mp3", StationGroup.MUSIC),
        RadioStation("Radio Paradise", "Eclectic rock", "https://stream.radioparadise.com/aac-320", StationGroup.MUSIC),
        RadioStation("KEXP Seattle", "Indie / alternative", "https://kexp.streamguys1.com/kexp160.aac", StationGroup.MUSIC),
    )
}