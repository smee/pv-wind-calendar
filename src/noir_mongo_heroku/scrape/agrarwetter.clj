(ns noir-mongo-heroku.scrape.agrarwetter
  (:use net.cgrand.enlive-html)
  (:require [noir-mongo-heroku.memoize :as m]))

(defn wetternet []
  (html-resource (java.net.URL. "http://www.wetter.net/47/Berlin"))) 

(def retrieve 
  (m/memoize 
    (fn [plz]
      (println "retrieving current weather data from proplanta for zipcode " plz)
      (let [template (str "http://www.proplanta.de/Solarwetter/profi-wetter.php?SITEID=60123&PLZ=%s&STADT=&WETTERaufrufen=postleitzahl&Wtp=SOLAR&SUCHE=Wetter&wT=%d")
            url1 (java.net.URL. (format template plz 0))
            url2 (java.net.URL. (format template plz 4))] 
        (list (html-resource url1)
              (html-resource url2))))
    (m/ttl-cache-strategy (* 2 60 60 1000))))

(defn get-dates [res]
  (distinct (map text (select res [:td :font.SCHRIFT_FORMULAR_WERTE_MITTE :b]))))

(def ^:private df (java.text.SimpleDateFormat. "dd.MM.yyyy"))

(defn- date2millis [s]
  (.getTime (.parse df s)))

(def ^:private tf (java.text.SimpleDateFormat. "dd.MM.yyyy HH:mm"))

(defn- time2millis [date time]
  (.getTime (.parse tf (str date " " (subs time 0 5)))))

(defn get-forecast [res]
  (let [current-measures? (not-empty (select res [(text-pred #(.contains % "Wetterzustand"))]))
        parts (->> (select res [:td :font.SCHRIFT_FORMULAR_WERTE_MITTE])
                (map text)
                (drop (if current-measures? 5 0)))
        num-dates (count (take-while #(re-matches #"\d{2}\.\d{2}\.\d{4}[\w]*" %) parts))
        parts (partition num-dates parts)
        [daily parts] (split-at 3 parts)
        [three-hours sun-times] (split-at 9 parts)]
    (for [i (range num-dates) :let [f #(nth % i)
                            date-str (subs (f (first daily)) 0 10)
                            millis (date2millis date-str)]]
      {:date millis
       :relative-sun (f (second daily))
       :estimated-gain (f (last daily))
       :sunrise (->> 1 (nth sun-times) f (time2millis date-str))
       :sunset (->> 2 (nth sun-times) f (time2millis date-str))
       :dawn (->> 3 (nth sun-times) f (time2millis date-str))
       :dusk (->> 4 (nth sun-times) f (time2millis date-str))
       :occlusions (zipmap (map #(vector (+ millis (* % 60 60 1000))
                                         (+ millis (* (+ 3 %) 60 60 1000))) (range 0 24 3)) 
                            (map f (rest three-hours)))})))
(defn wetternet [res]
  (let [hourly-data (select res [(attr-starts :id "prognose_")])
        times (select h [:div.zeit_inner :span text])
        labels-fn #(select % [:span.werte_headline text])
        values-fn #(select % [:span.werte_daten text])
        labels (map labels-fn hourly-data)
        values (map values-fn hourly-data)]
   (zipmap times (map zipmap labels values))))

