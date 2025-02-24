(ns alko.core
  (:require [dk.ative.docjure.spreadsheet :as d]
            [jsonista.core :as j]
            [clojure.java.io :as io]
            [clojure.string :as str]))



(defn select-indices [coll indices]
  (mapv #(nth coll %) indices))

(defn alko-sort [coll] (->> coll
                            (sort-by #(Double/parseDouble (nth % 7)) >)
                            (mapv #(conj %2 (str %1)) (next (range)))))

(def alko-seq (->> (d/load-workbook "alkon-hinnasto-tekstitiedostona.xlsx")
                   (d/select-sheet "Alkon Hinnasto Tekstitiedostona")
                   (d/row-seq)
                   (drop 4)))

(def cell-parser (comp
                  (map d/cell-seq)
                  (map #(mapv d/read-cell %))
                  (map #(select-indices % [0 1 3 4 5 8 21]))
                  (remove #(or (= (nth % 5) "lahja- ja juomatarvikkeet")
                               (= (nth % 5) "alkoholittomat")))
                  (map #(assoc % 2 (str/replace (nth % 2) #" l" "")))
                  (map #(conj % (format "%.4f" (/ (Double/parseDouble (nth % 6))
                                                  (Double/parseDouble (nth % 4))))))
                  (map #(conj % (str "<a href=\"https://www.alko.fi/tuotteet/"
                                     (nth % 0)
                                     "\"
                                      target=\"_blank\">"
                                     (nth % 1)
                                     "</a>"))) 
                  ))

(alko-sort (into '[] cell-parser alko-seq))


;; TODO hiccup
(defn generate-html [input-data]
  (let [json-data (j/write-value-as-string input-data)
        template (slurp "template.html")
        html-content (clojure.string/replace template "{{ data_placeholder }}" json-data)] 
    (with-open [writer (io/writer "index.html")]
      (.write writer html-content))))

;; TODO create some usefull cli interface here
