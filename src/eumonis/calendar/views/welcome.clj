(ns eumonis.calendar.views.welcome
  (:require [eumonis.calendar 
             [scrape :as sc]
             [icalendar :as ical]]
            [eumonis.calendar.views.common :as common]
            )
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        [noir.response :only (redirect content-type)]))

(defpage "/" []
  (redirect "/welcome"))

(defpage "/welcome" []
         (common/layout
           [:p "Please direct your calendar application to /cal/zipcode where zipcode is your local zipcode, for example see " [:a {:href "/solar/04155"} "this link"]]))

;;;;;;;;;;;;;;;;; solar forecast ;;;;;;;;;;;;;;;;;;;;;;;;
(defn overlaps? 
  "Do the numeric intervals overlap each other in any way?"
  [[s1 e1] [s2 e2]]
  (or (and (> s1 s2) (< s1 e2)) (and (> e1 s2) (< e1 e2))
      (and (> s2 s1) (< s2 e1)) (and (> e2 s1) (< e2 e1))))

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

(defpage "/solar/:plz" {plz :plz}
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

(defpage "/wind/:plz" {plz :plz}
  (let [forecasts (sc/get-wind-forecast plz)
        events (mapcat create-wind-events forecasts)]
    (content-type "text/calendar; charset=utf-8" (ical/create-icalendar events)))) 
