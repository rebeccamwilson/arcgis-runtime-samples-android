/* Copyright 2018 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.esri.arcgisruntime.sample.geodesicoperations

import java.util.Arrays

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import android.graphics.Color
import kotlinx.android.synthetic.main.activity_main.mapView

import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.esri.arcgisruntime.geometry.GeodeticCurveType
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.Polyline
import com.esri.arcgisruntime.geometry.PointCollection
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.geometry.LinearUnit
import com.esri.arcgisruntime.geometry.LinearUnitId
import com.esri.arcgisruntime.geometry.Point

class MainActivity : AppCompatActivity() {

  private val srWgs84 = SpatialReferences.getWgs84()
  private val unitOfMeasurement = LinearUnit(LinearUnitId.KILOMETERS)
  private val units = "Kilometers"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // create a map
    val map = ArcGISMap(Basemap.createImagery())

    // set a map to a map view
    mapView.map = map

    // create a graphic overlay
    val graphicOverlay = GraphicsOverlay()
    mapView.graphicsOverlays.add(graphicOverlay)

    // add a graphic at JFK to represent the flight start location
    val start = Point(-73.7781, 40.6413, srWgs84)
    val locationMarker = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFF0000FF.toInt(), 10f)
    val startLocation = Graphic(start, locationMarker)

    // create a graphic for the destination
    val endLocation = Graphic()
    endLocation.symbol = locationMarker

    // create a graphic representing the geodesic path between the two locations
    val path = Graphic()
    path.symbol = SimpleLineSymbol(SimpleLineSymbol.Style.DASH, 0xFF0000FF.toInt(), 5f)

    // add graphics to graphics overlay
    graphicOverlay.graphics.apply {
      add(startLocation)
      add(endLocation)
      add(path)
    }

    // create listener to get the location of the tap in the screen
    mapView.onTouchListener = object : DefaultMapViewOnTouchListener(this, mapView) {
      override fun onSingleTapConfirmed(e: MotionEvent): Boolean {

        // get the point that was clicked and convert it to a point in the map
        val clickLocation = android.graphics.Point(Math.round(e.x), Math.round(e.y))
        val mapPoint = mapView.screenToLocation(clickLocation)
        val destination = GeometryEngine.project(mapPoint, SpatialReferences.getWgs84())
        endLocation.geometry = destination

        // create a straight line path between the start and end locations
        val points = PointCollection(Arrays.asList<Point>(start, destination as Point), srWgs84)
        val polyLine = Polyline(points)

        // densify the path as a geodesic curve with the path graphic
        val pathGeometry = GeometryEngine.densifyGeodetic(polyLine, 1.0, unitOfMeasurement, GeodeticCurveType.GEODESIC)
        path.geometry = pathGeometry

        // calculate path distance
        val distance = GeometryEngine.lengthGeodetic(pathGeometry, unitOfMeasurement, GeodeticCurveType.GEODESIC)

        // create a textView for the callout
        val calloutContent = TextView(applicationContext)
        calloutContent.setTextColor(Color.BLACK)
        calloutContent.setSingleLine()

        // format coordinates to 2 decimal places
        val distanceString = String.format("%.2f", distance)

        // display distance as a callout
        calloutContent.text = "Distance: $distanceString $units"
        val callout = mapView.callout
        callout.location = mapPoint
        callout.content = calloutContent
        callout.show()
        return true
      }
    }
  }

  override fun onPause() {
    super.onPause()
    mapView.pause()
  }

  override fun onResume() {
    super.onResume()
    mapView.resume()
  }

  override fun onDestroy() {
    super.onDestroy()
    mapView.dispose()
  }
}
