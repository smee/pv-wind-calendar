(ns eumonis.calendar.views.welcome
  (:require [eumonis.calendar 
             [scrape :as sc]
             [icalendar :as ical]]
            [eumonis.calendar.views.common :as common]
            [noir.options :as opt]
            [clj-cache.cache :as cache]
            )
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        [noir.response :only (redirect content-type)]))

(defpage "/" []
  (redirect (url-for welcome)))

(defpage welcome "/welcome" []
  (common/layout
    ;; TODO to prepend the context root (base url) wrap the (url-for...) form in a (hiccup.core/resolve-url ...)
    ;; but: defpage ignores the base-url, so it won't work at all :(
    [:p "Please direct your calendar application to /cal/zipcode where zipcode is your local zipcode, for example see " [:a {:href (opt/resolve-url (url-for solar {:plz "04155"}))} "this link"]]))


(defpage "/huhu" []
  "huhu")

;;;;;;;;;;;;;;;;; solar forecast ;;;;;;;;;;;;;;;;;;;;;;;;
(defn overlaps? 
  "Do the numeric intervals overlap each other in any way?"
  [[s1 e1] [s2 e2]]
  (or (< s2 s1 e2) (< s2 e1 e2)
      (< s1 s2 e1) (< s1 e2 e1)))

(defn- create-solar-events [{:keys [sunset date dawn dusk sunrise occlusions relative-sun estimated-gain]}]
    (concat
      (vector
        (ical/create-allday-event date (str "rel. Sonnenscheindauer: " relative-sun))
        (ical/create-allday-event date (str "Globalstrahlung: " estimated-gain))
        (ical/create-event dawn sunrise "Dämmerungsanfang bis Sonnenaufgang")
        (ical/create-event sunset dusk "Sonnenuntergang bis Dämmerungsende"))
      (for [[[start end] text] (sort-by ffirst occlusions) 
            :when (or (overlaps? [start end] [dawn sunrise])
                      (overlaps? [start end] [sunset dusk])
                      (overlaps? [start end] [sunrise sunset]))
            :let [start (max start sunrise)
                  end (min end sunset)]]
        (ical/create-event start end text))))

(defpage solar "/solar/:plz" {plz :plz}
  (let [forecasts (sc/get-solar-forecast plz)
        events (mapcat create-solar-events forecasts)]
    (content-type "text/calendar; charset=utf-8" (ical/create-icalendar events))))

;;;;;;;;;;;;;;;;; wind forecast ;;;;;;;;;;;;;;;;;;;;;;;;

(defn- create-wind-events [m]
  (let [time-diff (Math/abs (- (second (keys m)) (first (keys m))))]
    (for [[t entries] (sort-by first (seq m))
          :let [text (format "%s aus %s (Böen bis %s)" 
                             (entries "Mittlere Windgeschw.") 
                             (entries "Windrichtung") 
                             (entries "Maximale Windböen"))]]
      (ical/create-event t (+ t time-diff) text)))
  )

(defpage wind "/wind/:plz" {plz :plz}
  (let [forecasts (sc/get-wind-forecast plz)
        events (mapcat create-wind-events forecasts)]
    (content-type "text/calendar; charset=utf-8" (ical/create-icalendar events)))) 

(defpage "/topsecret/reset-wind-cache" []
  (cache/invalidate-cache sc/get-wind-forecast)
  "Done.")

(defpage "/topsecret/reset-solar-cache" []
  (cache/invalidate-cache sc/get-solar-forecast)
  "Done.")