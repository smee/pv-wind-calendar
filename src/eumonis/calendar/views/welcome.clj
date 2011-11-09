(ns eumonis.calendar.views.welcome
  (:require [eumonis.calendar 
             [scrape :as sc]
             [icalendar :as ical]]
            [eumonis.calendar.views.common :as common]
            )
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        [noir.response :only (redirect)]))

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
  (let [adjusted-date (+ date (* 60 60 1000))];; XXX prevent mistakes in daylight saving time, midnight of a date plus one hour should be still the same day 
    ;; TODO use time zone informations!
    (concat
      (vector
        (ical/create-allday-event adjusted-date (str "rel. Sonnenscheindauer: " relative-sun))
        (ical/create-allday-event adjusted-date (str "Globalstrahlung: " estimated-gain))
        (ical/create-event dawn sunrise "Dämmerungsanfang bis Sonnenaufgang")
        (ical/create-event sunset dusk "Sonnenuntergang bis Dämmerungsende"))
      (for [[[start end] text] occlusions 
            :when (or (overlaps? [start end] [dawn sunrise])
                      (overlaps? [start end] [sunset dusk])
                      (overlaps? [start end] [sunrise sunset]))
            :let [start (max start sunrise)
                  end (min end sunset)]]
        (ical/create-event start end text)))))

(defpage "/solar/:plz" {plz :plz}
  (let [forecasts (sc/get-solar-forecast plz)
        events (mapcat create-solar-events forecasts)]
    (ical/create-icalendar events)))

;;;;;;;;;;;;;;;;; wind forecast ;;;;;;;;;;;;;;;;;;;;;;;;

(defn- create-wind-events [m]
  (let [time-diff (- (second (keys m)) (first (keys m)))]
    (for [[t entries] m
          :let [text (format "%s aus %s (Böen bis %s)" 
                             (entries "Mittlere Windgeschw.") 
                             (entries "Windrichtung") 
                             (entries "Maximale Windböen"))]]
      (ical/create-event t (+ t time-diff) text)))
  )

(defpage "/wind/:plz" {plz :plz}
  (let [forecasts (sc/get-wind-forecast plz)
        events (mapcat create-wind-events forecasts)]
    (ical/create-icalendar events))) 