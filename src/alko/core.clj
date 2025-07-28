(ns alko.core
  (:require [dk.ative.docjure.spreadsheet :as d]
            [jsonista.core :as j]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.core :as h]))



(defn select-indices
  [coll indices]
  (mapv #(nth coll %) indices))

(defn alko-sort
  [coll]
  (->> coll
       (sort-by #(Double/parseDouble (nth % 7)) >)
       (mapv #(conj %2 (str %1)) (next (range)))))

;; todo, move to function, might be more usefull in cli app
(def alko-seq
  (->> (d/load-workbook "alkon-hinnasto-tekstitiedostona.xlsx")
       (d/select-sheet "Alkon Hinnasto Tekstitiedostona")
       (d/row-seq)
       (drop 4)))

(def cell-parser
  (comp
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
                      "\"target=\"_blank\">"
                      (nth % 1)
                      "</a>")))))

(time (alko-sort (into '[] cell-parser alko-seq)))


;; TODO hiccup
(defn site
  [data]
  (h/html [:meta {:charset "UTF-8"}]
          [:meta
           {:name "viewport", :content "width=device-width, initial-scale=1.0"}]
          [:title "Alko Price List"]
          [:link {:rel "stylesheet" :href "https://cdn.datatables.net/1.13.4/css/jquery.dataTables.min.css"}]
          [:script {:src "https://code.jquery.com/jquery-3.6.0.min.js"}]
          [:script {:src "https://cdn.datatables.net/1.13.4/js/jquery.dataTables.min.js"}]
          [:style
           "h1 { text-align: center; } table { width: 100%; margin-top: 20px; }"]
          [:h1 "Alko Price List"]
          [:table
           {:id "data-table" :class "display"}
           [:thead
            [:tr
             [:th "Rank"]
             [:th "Product Name"]
             [:th "Volume (L)"]
             [:th "Price (€)"]
             [:th "Price (€) Per Volume (L)"]
             [:th "Category"]
             [:th "Alcohol (%)"]
             [:th "Alcohol (L) Per Price (€)"]]]
           [:tbody]]
          [:script
           (str "$(document).ready(function() {
    const tableData = " data " ;
    $('#data-table').DataTable({
        data: tableData,
        columns: [
            { title: \"Rank\" },
            { title: \"Product Name\" },
            { title: \"Volume (L)\" },
            { title: \"Price (€)\" },
            { title: \"Price (€) Per Volume (L)\" },
            { title: \"Category\" },
            { title: \"Alcohol (%)\" },
            { title: \"Alcohol (L) Per Price (€)\" }
        ],
        pageLength: 14, 
    });
    });")]))

(site (alko-sort (into '[] cell-parser alko-seq)))

(defn generate-html [input-data]
  (let [json-data (j/write-value-as-string input-data)
        template (slurp "template.html")
        html-content (clojure.string/replace template "{{ data_placeholder }}" json-data)] 
    (with-open [writer (io/writer "index.html")]
      (.write writer html-content))))

;; TODO create some usefull cli interface here
