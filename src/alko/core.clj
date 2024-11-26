(ns alko.core
  (:require [dk.ative.docjure.spreadsheet :as d]
            [jsonista.core :as j]
            [clojure.java.io :as io]
            [clojure.string :as s]))


;; TODO move to functions that read excel and then transducer that parses it using sequence?? maybe
(def data (->> (d/load-workbook "alkon-hinnasto-tekstitiedostona.xlsx")
               (d/select-sheet "Alkon Hinnasto Tekstitiedostona")
               (d/select-columns {:A :id
                                  :B :name
                                  :D :size
                                  :E :price
                                  :F :price-per-liter
                                  :I :type
                                  :V :alkohol})
               (drop 4)
               (filter #(and (not= (:type %) "lahja- ja juomatarvikkeet")
                             (not= (:type %) "alkoholittomat")))
               (map #(assoc % :size (s/replace (s/replace (:size %) #" l" "") #"," ".")))
               (map #(assoc % :apk (if (not= (:price-per-liter %) nil)
                                     (/ (Double/parseDouble (:alkohol %))
                                        (Double/parseDouble (:price-per-liter %)))
                                     0)))
               (sort-by :apk >)
               (map #(assoc %2 :rank %1) (next (range)))
               (map #(assoc % :name (str "<a href=\"https://www.alko.fi/tuotteet/" (:id %) "\" target=\"_blank\">" (:name %) "</a>")))
               (map (juxt :rank :name :size :price :price-per-liter :type :alkohol :apk))
               ))

;; TODO dont use slurp to read template
(defn generate-html [input-data]
  (let [json-data (j/write-value-as-string input-data)
        template (slurp "template.html")
        html-content (clojure.string/replace template "{{ data_placeholder }}" json-data)] 
    (with-open [writer (io/writer "index.html")]
      (.write writer html-content))))

;; TODO create some usefull cli interface here
(defn -main []
  (generate-html data)
  (println "hello world"))


(comment
  (count data)
  (generate-html data)
  data
  (take 10 data)
  (nth data 100)
  (last data)
  (first data)
  )