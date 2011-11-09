(ns eumonis.calendar.icalendar
  (:import [net.fortuna.ical4j.model Calendar Property Date DateTime]
           [net.fortuna.ical4j.model.property ProdId Version CalScale]
           net.fortuna.ical4j.model.component.VEvent
           net.fortuna.ical4j.model.parameter.Value
           net.fortuna.ical4j.util.UidGenerator))

(def ^:private uid-gen (UidGenerator. "1"))
(def ^:private local-tz (.getTimeZone (.createRegistry (net.fortuna.ical4j.model.TimeZoneRegistryFactory/getInstance))
                                      "Europe/Berlin"))

(defn create-allday-event [date text]
  (let [adjusted-date (+ date (* 60 60 1000))] ;; XXX prevent mistakes in daylight saving time, midnight of a date plus one hour should be still the same day
    (doto (VEvent. (Date. adjusted-date) text)
      (.. getProperties (add (.generateUid uid-gen))))))

(defn create-event [start end text]
  (let [start (doto (DateTime. start) (.setTimeZone local-tz))
        end (doto (DateTime. end) (.setTimeZone local-tz))] 
    (doto (VEvent. start end text)
      (.. getProperties (add (.generateUid uid-gen))))))


(defn create-icalendar [events]
  (let [cal (Calendar.)
        now (System/currentTimeMillis)
        output (net.fortuna.ical4j.data.CalendarOutputter.)
        buf (java.io.StringWriter.)]
    (doto (.getProperties cal)
            (.add (ProdId. "-//Steffen Dienst//EUMONIS Test 1.0//EN"))
            (.add Version/VERSION_2_0)
            (.add CalScale/GREGORIAN))
    (.. cal getComponents (addAll events))
    (.output output cal buf)
    (str buf)))