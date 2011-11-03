(ns noir-mongo-heroku.views.welcome
  (:require [noir-mongo-heroku.views.common :as common]
            [noir.content.pages :as pages])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers))

(defpage "/welcome" []
         (common/layout
           [:p "Welcome to noir-mongo-heroku"]))
