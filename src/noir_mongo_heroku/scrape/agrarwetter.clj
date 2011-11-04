(ns noir-mongo-heroku.scrape.agrarwetter
  (:use net.cgrand.enlive-html))

(defn retrieve [plz]
  (html-resource (java.net.URL. (str "http://www.proplanta.de/Solarwetter/profi-wetter.php?SITEID=60123&PLZ=" plz "&STADT=&WETTERaufrufen=postleitzahl&Wtp=SOLAR&SUCHE=Wetter&wT=0"))))

(defn get-dates [res]
  (distinct (map text (select res [:td :font.SCHRIFT_FORMULAR_WERTE_MITTE :b]))))

(defn get-occlusion-day [res]
  (let [current-measures? (not-empty (select x [(text-pred #(.contains % "Wetterzustand"))]))
        parts (->> (select res [:td :font.SCHRIFT_FORMULAR_WERTE_MITTE])
                (map text)
                (drop (if current-measures? 5 0))
                (partition 4))
        [daily parts] (split-at 3 parts)
        [three-hours sun-times] (split-at 9 parts)]
    ;; return map of date string to relative sunshine duration
    (println daily)
    (zipmap (map #(subs % 0 10) (first daily)) (second daily))
    
    ))