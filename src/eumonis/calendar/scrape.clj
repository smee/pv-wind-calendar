(ns eumonis.calendar.scrape
  (:use net.cgrand.enlive-html)
  (:require [eumonis.calendar.memoize :as m])
  (:import java.util.Calendar
           java.net.URL))

;;;;;;;;;;;;;; parse solar wheather from proplanta.de/Solarwetter ;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- retrieve-agrarwetter [plz-or-city]
  (println "retrieving current solar weather data from proplanta for zipcode/city " plz-or-city)
  (let [template (str "http://www.proplanta.de/Solarwetter/profi-wetter.php?SITEID=60123&PLZ=%s&STADT=&WETTERaufrufen=postleitzahl&Wtp=SOLAR&SUCHE=Wetter&wT=%d")
        url1 (URL. (format template plz-or-city 0))
        url2 (URL. (format template plz-or-city 4))] 
    (list (html-resource url1)
          (html-resource url2))))

(def ^:private tf (java.text.SimpleDateFormat. "dd.MM.yyyy HH:mm"))

(defn- time2millis [date time]
  (.getTime (.parse tf (str date " " (subs time 0 5)))))

(defn- extract-agrarwetter [res]
  (let [df (java.text.SimpleDateFormat. "dd.MM.yyyy")
        measures-avail? (not-empty (select res [(text-pred #(.contains % "Wetterzustand"))]))
        parts (->> (select res [:td :font.SCHRIFT_FORMULAR_WERTE_MITTE])
                (map text)
                (drop (if measures-avail? 5 0)))
        num-dates (count (take-while #(re-matches #"\d{2}\.\d{2}\.\d{4}[\w]*" %) parts))
        parts (partition num-dates parts)
        [daily parts] (split-at 3 parts)
        [three-hours sun-times] (split-at 9 parts)]
    
    (for [i (range num-dates) :let [f #(nth % i)
                                    date-str (subs (f (first daily)) 0 10)
                                    millis (.getTime (.parse df date-str))]]
      {:date millis
       :relative-sun (f (second daily))
       :estimated-gain (f (last daily))
       :sunrise (->> 1 (nth sun-times) f (time2millis date-str))
       :sunset  (->> 2 (nth sun-times) f (time2millis date-str))
       :dawn    (->> 3 (nth sun-times) f (time2millis date-str))
       :dusk    (->> 4 (nth sun-times) f (time2millis date-str))
       :occlusions (zipmap (map #(vector (+ millis (* % 60 60 1000))
                                         (+ millis (* (+ 3 %) 60 60 1000))) (range 0 24 3)) 
                           (map f (rest three-hours)))})))

(defn get-solar-forecast [plz-or-city] 
  (mapcat extract-agrarwetter (retrieve-agrarwetter plz-or-city)))

;;;;;;;;;;;;;; parse wheather from wetter.net ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- wetternet-details [res midnight-time]
  (let [hourly-data (select res [(attr-starts :id "prognose_")])
        tf (java.text.SimpleDateFormat. "HH:mm")
        times (->> [:div.zeit_inner :span text]
                (select res)
                (map #(.getTime (.parse tf %)))
                (map (partial + midnight-time)))
        labels-fn #(select % [:span.werte_headline text])
        values-fn #(select % [:span.werte_daten text])
        labels (map labels-fn hourly-data)
        values (map values-fn hourly-data)]
   (zipmap times (map zipmap labels values))))

(defn- get-midnight 
  "Get epoch time of today's midnight."
  []
   (let [cal (doto (Calendar/getInstance)
               (.set Calendar/HOUR_OF_DAY 0)
               (.set Calendar/MINUTE 0)
               (.set Calendar/SECOND 0)
               (.set Calendar/MILLISECOND 0))]
     (.getTimeInMillis cal)))

(defn get-wind-forecast [plz-or-city]
  (let [main-url (str "http://www.wetter.net/cgi-bin/wetter-net3/wetter-stadt.pl?NAME=" plz-or-city)
        main-res (html-resource (URL. main-url))
        detail-links (->> [:div.wetterwerte_vue :div.details_unten ] 
                       (select main-res)
                       (map #(-> % :attrs :onclick))
                       (map #(subs % (count "location.href='") (dec (count %)))))
        today-midnight (get-midnight)
        detail-res (map #(html-resource (URL. %)) detail-links)
        details (map #(wetternet-details % (+ today-midnight (* %2 24 60 60 1000))) detail-res (range (count detail-res)))
        ]
    details)) 


;; cache extracted solar forecast for 3 hours
(alter-var-root #'get-solar-forecast m/memoize (m/ttl-cache-strategy (* 3 60 60 1000)))
;; cache extracted wind forecast for 3 hours
(alter-var-root #'get-wind-forecast m/memoize (m/ttl-cache-strategy (* 3 60 60 1000)))

