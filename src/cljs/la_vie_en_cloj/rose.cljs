(ns la-vie-en-cloj.rose
  (:require [mondrian.anim :as anim]
            [mondrian.canvas :as canvas]
            [mondrian.color :as color]
            [mondrian.math :as math]
            [mondrian.plot :as plot]
            [mondrian.ui :as ui]
            [monet.canvas :as m])
  (:use-macros [dommy.macros :only [sel1]]))

;; ---------------------------------------------------------------------
;; Update pipeline
;;
;; Initial state:
;;    :drawing -- The top-level DOM element that contains the canvas,
;;      the controls, etc.
;;    :ctx -- The canvas context.
;;    :w -- The width of the canvas.
;;    :h -- The height of the canvas.
;;
;; Configuration values (as defined on the mondrian element):
;;    :fixed-r -- The radius of the fixed circle.
;;    :rolling-r -- The radius of the rolling circle.
;;    :radial -- The length of the radial.
;;    :num-points -- The number of points to draw.
;;    :point-rpm -- The speed at which the rolling circle moves around
;;      the fixed circle.
;;    :persist-image -- Whether or not to clear the background in
;;      between frames.
;;    :show-wireframe -- Whether or not to draw the explanatory fixed
;;      circle, rolling circle, and radial.
;;
;; After merge-control-values:
;;    :... -- One key for each control (identified by the control name).
;;      Zero or more controls may be present under the mondrian element,
;;      in which case the missing values must have been provided in as
;;      defaults in the element itself.
;;
;; After scale-to-canvas:
;;    :fixed-r, rolling-r, radial -- Scaled version of the original
;;      values; ensures that the epitrochoid fills the entire canvas.
;;
;; After update-point-step:
;;    :point-step -- The offset (in radians) to apply to theta; causes
;;      the points to move along the path of the epitrochoid.

(defn merge-control-values
  "Merge the current values of the controls into state."
  [{:keys [drawing] :as state}]
  (merge state (ui/update-controls drawing)))

(defn scale-to-canvas
  "Computes the scaling factor required to make the epitrochoid fill the
  canvas and applies that scaling factor to the input values of the
  epitrochoid equation (:fixed-r, :rolling-r, and :radial)."
  [{:keys [w h fixed-r rolling-r radial] :as state}]
  (let [frame-radius (/ (min w h) 2)
        epi-radius (+ fixed-r rolling-r radial 1) ;; 1 for the point itself
        scale (/ frame-radius epi-radius)]
    (assoc state
           :fixed-r (* fixed-r scale)
           :rolling-r (* rolling-r scale)
           :radial (* radial scale))))

(defn update-point-step
  "Updates point-step by applying the RPM value after considering how
  much movement needs to have occurred since the last call to
  update-point-step."
  [{:keys [delta-t-ms point-rpm] :as state}]
  (let [point-radians-per-minute (* point-rpm 2 Math/PI)
        point-radians-per-millisecond (/ point-radians-per-minute 60000)
        delta-point-radians (* delta-t-ms point-radians-per-millisecond)]
    (update-in state [:point-step] #(+ (or % 0) delta-point-radians))))

(defn update-pipeline
  [state]
  (-> state
      merge-control-values
      scale-to-canvas
      update-point-step))


;; ---------------------------------------------------------------------
;; Render stack
;;

(defn clear-background
  [{:keys [ctx w h persist-image]}]
  (when-not persist-image
    (-> ctx
        (m/fill-style "rgba(25,29,33,0.75)") ;; Alpha adds motion blur
        (m/fill-rect {:x 0 :y 0 :w w :h h}))))

(defn draw-fixed-circle
  [ctx {:keys [fixed-r]}]
  (m/stroke-style ctx "#333")
  (canvas/stroke-circle ctx {:x 0 :y 0 :r fixed-r}))

(defn draw-rolling-circle
  [ctx {:keys [fixed-r rolling-r point-step]}]
  (let [cent-x (math/circle-x (+ fixed-r rolling-r) point-step)
        cent-y (math/circle-y (+ fixed-r rolling-r) point-step)]
    (m/stroke-style ctx "#666")
    (canvas/stroke-circle ctx {:x cent-x :y cent-y :r rolling-r})))

(defn draw-radial
  [ctx {:keys [fixed-r rolling-r radial point-step]}]
  (let [cent-x (math/circle-x (+ fixed-r rolling-r) point-step)
        cent-y (math/circle-y (+ fixed-r rolling-r) point-step)
        epi-x (math/epitrochoid-x fixed-r rolling-r radial point-step)
        epi-y (math/epitrochoid-y fixed-r rolling-r radial point-step)]
    (m/stroke-style ctx "#999")
    (canvas/stroke-line ctx cent-x cent-y epi-x epi-y)))

(defn draw-wireframe
  [{:keys [ctx w h show-wireframe] :as state}]
  (when show-wireframe
    (-> ctx
        m/save
        canvas/translate-to-center
        (draw-fixed-circle state)
        (draw-rolling-circle state)
        (draw-radial state)
        m/restore)))

(defn point-size
  "Returns the size of each animated point in the drawing based on the
  overall size of the canvas."
  [w h]
  (let [shortest-side (min w h)
        relative-point-size (* 0.005 shortest-side)
        floored-point-size (max relative-point-size 2)]
    floored-point-size))

(defn draw-epitrochoid
  [{:keys [ctx w h fixed-r rolling-r radial num-points point-step]}]
  (-> ctx
      m/save
      canvas/translate-to-center
      (plot/parametric-equation #(math/epitrochoid-x
                                   fixed-r rolling-r radial (+ % point-step))
                                #(math/epitrochoid-y
                                   fixed-r rolling-r radial (+ % point-step))
                                color/hsl-by-hue-rad
                                num-points
                                (point-size w h))
      m/restore))

(defn render-stack
  [state]
  (clear-background state)
  (draw-wireframe state)
  (draw-epitrochoid state))


;; ---------------------------------------------------------------------
;; Main entry point
;;

(defn ^:export start
  "Starts the epitrochoid animation given the top-level DOM element that
  contains the canvas and any controls."
  [drawing]
  (let [canvas (sel1 drawing :canvas)
        ctx (m/get-context canvas "2d")
        [w h] (ui/setup-canvas canvas ctx 1.0)
        state {:drawing drawing :ctx ctx :w w :h h}]
    (anim/start state
                #(update-pipeline %)
                #(render-stack %)
                #(-> ctx
                     (m/fill-style "red")
                     (m/font-style "sans-serif")
                     (m/text {:text % :x 0 :y 20})))))
