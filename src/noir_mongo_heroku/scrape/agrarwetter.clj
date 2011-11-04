(ns noir-mongo-heroku.scrape.agrarwetter
  (:use net.cgrand.enlive-html))

(defn- retrieve [plz]
  (html-resource (java.net.URL. (str "http://www.proplanta.de/Solarwetter/profi-wetter.php?SITEID=60123&PLZ=" plz "&STADT=&WETTERaufrufen=postleitzahl&Wtp=SOLAR&SUCHE=Wetter&wT=0"))))