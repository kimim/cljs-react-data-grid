(ns vorstellung.data-grid
  (:require [clojure.string :as s]
            [reagent.core :as r]
            ["faker" :as faker]
            ["react-select" :as select :default Select]
            ["react-sortable-hoc" :as sortable]
            ["lodash" :as lodash]
            ["react-data-grid" :as rdg :default DataGrid]))

(set! faker/locale "zh_CN")

(defn fake-data [index]
  {:id index
   :avatar (.avatar faker/image)
   :name (str (.lastName faker/name) (.firstName faker/name))
   :gender (get ["男" "女"] (rand-int 2))
   :job-grade (get ["高级工程师" "中级工程师" "初级工程师" "无"] (rand-int 4))
   :job-type (.jobType faker/name)
   :company (str (.firstName faker/name) (get ["科技" "教育" "公司" "传媒" "集团" "网络"] (rand-int 6)))
   :salary (+ 5000 (rand-int 50000))})

(def rows (for [i (range 1 1000)] (fake-data i)))

(defn create-filter [{:keys [key value filter-type]}]
  (case filter-type
    :eq (case value
          "All" (constantly true)
          #(= value (% key)))
    :contains #(s/includes? (% key) value)))

(defn employees []
  (r/with-let [filter-map
               (r/atom {:filter {:job-grade {:key :job-grade :value "All" :filter-type :eq}
                                 :gender {:key :gender :value "All" :filter-type :eq}
                                 :name {:key :name :value "" :filter-type :contains}}})]
    (let [filtered-rows
          (filter (apply every-pred (map create-filter (vals (@filter-map :filter)))) rows)
          cols [{:key :id :name "工号" :summaryFormatter #(r/as-element [:strong "合计："])}
                {:key :name :name "姓名"
                 :summaryFormatter #(r/as-element [:strong (count filtered-rows)])
                 :filterRenderer #(r/as-element [:div.rdg-filter-container
                                                 [:input.rdg-filter
                                                  {:value (get-in @filter-map [:filter :name :value])
                                                   :on-change
                                                   (fn [e] (let [val (.-value (.-target e))]
                                                             (swap! filter-map assoc-in [:filter :name :value] val)))}]])}
                {:key :avatar :name "照片" :width 40 :resizable true
                 :formatter
                 #(r/as-element
                   [:div.rdg-image-cell-wrapper
                    [:div.rdg-image-cell
                     {:style {:background-image (str "url(" (get-in (js->clj %) ["row" "avatar"]) ")")}}]])}
                {:key :gender :name "性别" :filterable true
                 :filterRenderer
                 (fn [] (r/as-element
                         [:div.rdg-filter-container
                          [:select.rdg-filter {:value (get-in @filter-map [:filter :gender :value])
                                               :on-change #(let [val (.-value (.-target %))]
                                                             (swap! filter-map assoc-in [:filter :gender :value] val))}
                           [:option {:value "All"} "All"]
                           [:option {:value "男"} "男"]
                           [:option {:value "女"} "女"]
                           ]]))}
                {:key :job-grade :name "职级" :filterable true
                 :filterRenderer
                 (fn [] (r/as-element
                         [:div.rdg-filter-container
                          [:select.rdg-filter {:value (get-in @filter-map [:filter :job-grade :value])
                                               :on-change #(let [val (.-value (.-target %))]
                                                             (swap! filter-map assoc-in [:filter :job-grade :value] val))}
                           [:option {:value "All"} "All"]
                           [:option {:value "高级工程师"} "高级工程师"]
                           [:option {:value "中级工程师"} "中级工程师"]
                           [:option {:value "初级工程师"} "初级工程师"]
                           [:option {:value "无"} "无"]]]))}
                {:key :job-type :name "岗位"}
                {:key :company :name "公司"}
                {:key :salary :name "薪酬" :sortable true
                 :summaryFormatter #(r/as-element [:strong (reduce + (map :salary filtered-rows)) " 元"])}]]
      [:div
       [:> DataGrid {:columns cols
                     :rows filtered-rows
                     :rowsCount 50
                     :height (- (.-innerHeight js/window) 54)
                     :rowKey :id
                     :summaryRows [{:id "total_0" :totalCount (count filtered-rows) :salaryCount 200}]
                     :className "fill-grid"
                     :enableFilters true
                     }]])))
