(ns noir-mongo-heroku.views.welcome
  (:require [noir-mongo-heroku.views.common :as common]
            [noir.content.pages :as pages]
            [noir-mongo-heroku.scrape.agrarwetter :as wetter])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        [noir.response :only (redirect)])
  (:import [net.fortuna.ical4j.model Calendar Property Date DateTime]
           [net.fortuna.ical4j.model.property ProdId Version CalScale]
           net.fortuna.ical4j.model.component.VEvent
           net.fortuna.ical4j.model.parameter.Value
           net.fortuna.ical4j.util.UidGenerator))

(def ^:private uid-gen (UidGenerator. "1"))

(defpage "/" []
  (redirect "/welcome"))

(defpage "/welcome" []
         (common/layout
           [:p "Please direct your calendar application to /cal/zipcode where zipcode is your local zipcode, for example see " [:a {:href "/cal/04155"} "this link"]]))

(defn- create-allday-event [date text]
  (doto (VEvent. (Date. date) text)
    (.. getProperties (add (.generateUid uid-gen)))))

(defn- create-event [start end text]
  (doto (VEvent. (DateTime. start) (DateTime. end) text)
    (.. getProperties (add (.generateUid uid-gen)))))

(defn- create-calendar-events [{:keys [sunset date dawn dusk sunrise occlusions relative-sun estimated-gain]}]
  (concat
    (vector
      (create-allday-event date (str "rel. Sonnenscheindauer: " relative-sun))
      (create-allday-event date (str "Globalstrahlung: " estimated-gain))
      (create-event dawn sunrise "Dämmerungsanfang bis Sonnenaufgang")
      (create-event sunset dusk "Sonnenuntergang bis Dämmerungsende"))
    (for [[[start end] text] occlusions]
      (create-event start end text))))

(defpage "/cal/:plz" {plz :plz}
  (let [cal (Calendar.)
        _ (doto (.getProperties cal)
            (.add (ProdId. "-//Steffen Dienst//EUMONIS Test 1.0//EN"))
            (.add Version/VERSION_2_0)
            (.add CalScale/GREGORIAN))
        now (System/currentTimeMillis)
        forecasts (wetter/get-forecast (wetter/retrieve plz))
        events (mapcat create-calendar-events forecasts) 
        output (net.fortuna.ical4j.data.CalendarOutputter.)
        buf (java.io.StringWriter.)]
    (.. cal getComponents (addAll events))
    (.output output cal buf)
    (str buf)
    ))