(ns alko.core
  (:require [dk.ative.docjure.spreadsheet :as d]
            [jsonista.core :as j]
            [clojure.java.io :as io]
            [clojure.string :as s]))


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
               (map #(assoc % :apk (/ (Double/parseDouble (:alkohol %))
                                      (Double/parseDouble (:price-per-liter %)))))
               (map (juxt :name :size :price :price-per-liter :type :alkohol :apk :id))))

(defn generate-html [input-data]
  (let [json-data (j/write-value-as-string input-data)
        template (slurp "template.html")
        html-content (clojure.string/replace template "{{ data_placeholder }}" json-data)] 
    (with-open [writer (io/writer "index.html")]
      (.write writer html-content))))


(defn -main []
  (println "hello world"))


(comment
  (count data)
  (generate-html data)
  data
  (take 10 data))