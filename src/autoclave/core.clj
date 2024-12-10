(ns autoclave.core
  "Provides aliases to the important bits."
  (:require [autoclave.html :as html]
            [autoclave.json :as json]))

(def html-sanitize
  "Alias for html/sanitize."
  html/sanitize)

(def html-policy
  "Alias for html/policy."
  html/policy)

(def html-merge-policies
  "Alias for html/merge-policies."
  html/merge-policies)

(def json-sanitize
  "Alias for json/sanitize."
  json/sanitize)
