(ns autoclave.html
  "Wraps the OWASP HTML Sanitizer library."
  (:require [clojure.string :as string])
  (:import [org.owasp.html AttributePolicy
            ElementPolicy
            HtmlPolicyBuilder
            HtmlSanitizer
            HtmlSanitizer$Policy
            PolicyFactory
            Sanitizers]))

(defn read-flat-key-val-sequence
  "Reads a flat sequence of key and (optional) values into proper key-value
  pairs. Keys are identified by key? returning true."
  ([options]
     (read-flat-key-val-sequence options keyword?))
  ([options key?]
     (if (seq options)
       (loop [[k & tail] options, result []]
         (if (key? k)
           (let [[v tail] (if (key? (first tail))
                            [nil tail]
                            [(first tail) (rest tail)])]
             (if (seq tail)
               (recur tail (conj result [k v]))
               (conj result [k v])))
           (throw (Exception. (str "Expected key, got non-key '" k "'")))))
       (vector))))

(defn- attr-policy [f]
  (proxy [AttributePolicy] []
    (apply [element-name attr-name value] (f element-name attr-name value))))

(defn- element-policy [f]
  (proxy [ElementPolicy] []
    (apply [element-name attrs] (f element-name attrs))))

(defn- apply-attr-option
  [builder name args]
  (case name
    :globally (.globally builder)
    :matching
    (let [[f] args]
      (.matching builder (if (fn? f) (attr-policy f) f)))
    :on-elements
    (.onElements builder (into-array String args))
    ; unknown option
    (throw (Exception. (str "Unknown html-policy attribute option " name)))))

(defn- apply-attr-options
  "Apply a sequence of attribute options to an AttributeBuilder."
  [builder options]
  (let [options (read-flat-key-val-sequence options)]
    (doseq [[name args] options]
      (apply-attr-option builder name args))
    builder))

(defn- apply-builder-option
  [builder name args]
  (case name
    :allow-attributes
    (let [[names options] (split-with (comp not keyword?) args)]
      (-> builder
          (.allowAttributes (into-array String names))
          (apply-attr-options options)))
    :allow-common-block-elements
    (.allowCommonBlockElements builder)
    :allow-common-inline-formatting-elements
    (.allowCommonInlineFormattingElements builder)
    :allow-elements
    (let [[[f] names] (split-with fn? args)
          names (into-array String names)]
      (if f
        (.allowElements builder (element-policy f) names)
        (.allowElements builder names)))
    :allow-standard-url-protocols
    (.allowStandardUrlProtocols builder)
    :allow-styling
    (.allowStyling builder)
    :allow-text-in
    (.allowTextIn builder (into-array String args))
    :allow-url-protocols
    (.allowUrlProtocols builder (into-array String args))
    :allow-without-attributes
    (.allowWithoutAttributes builder (into-array String args))
    :disallow-attributes
    (let [[names options] (split-with (comp not keyword?) args)]
      (-> builder
          (.disallowAttributes (into-array String names))
          (apply-attr-options options)))
    :disallow-elements
    (.disallowElements builder (into-array String args))
    :disallow-text-in
    (.disallowTextIn builder (into-array String args))
    :disallow-url-protocols
    (.disallowUrlProtocols builder (into-array String args))
    :disallow-without-attributes
    (.disallowWithoutAttributes builder (into-array String args))
    :require-rel-nofollow-on-links
    (.requireRelNofollowOnLinks builder)
    ;; unknown option
    (throw (Exception. (str "Unknown html-policy option " name)))))

(defn- apply-builder-options
  [builder options]
  (doseq [[name args] (read-flat-key-val-sequence options)]
    (apply-builder-option builder name args))
  builder)

(def predefined-policies
  {:BLOCKS      Sanitizers/BLOCKS
   :FORMATTING  Sanitizers/FORMATTING
   :IMAGES      Sanitizers/IMAGES
   :LINKS       Sanitizers/LINKS
   :STYLES      Sanitizers/STYLES})

(defn policy
  "Access a predefined policy or create one from a sequence of options."
  [& options]
  (or (predefined-policies (first options))
      (.toFactory (apply-builder-options (HtmlPolicyBuilder.) options))))

(defn- policy-factory
  "Coerce argument to a PolicyFactory, which supports merging."
  [p]
  (cond
    (keyword? p) (apply policy (conj [] p))
    (instance? PolicyFactory p) p
    :else (throw (Exception. (str (class p) " does not support merging.")))))

(defn merge-policies
  "Merge multiple PolicyFactory and/or option sequences together."
  [& policies]
  (->> policies (map policy-factory) (reduce #(.and %1 %2))))

(defn sanitize
  "Apply an HTML sanitization policy (a PolicyFactory object rather than a
   sequence of options) to a string of HTML."
  ([html] (sanitize (policy) html))
  ([policy html] (.sanitize policy html)))
