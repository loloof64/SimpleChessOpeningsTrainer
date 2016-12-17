/**
  * Simple chess openings trainer
  * Copyright (C) 2016  Laurent Bernabe
  **
  *This program is free software: you can redistribute it and/or modify
  *it under the terms of the GNU General Public License as published by
  *the Free Software Foundation, either version 3 of the License, or
  *(at your option) any later version.
  **
  *This program is distributed in the hope that it will be useful,
  *but WITHOUT ANY WARRANTY; without even the implied warranty of
  *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *GNU General Public License for more details.
  **
  *You should have received a copy of the GNU General Public License
  *along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */

import scalafx.scene.Group
import scalafx.scene.shape.Rectangle
import scalafx.scene.paint.Color._
import scalafx.scene.text.{Font, FontWeight, Text}

class BoardGroup(cellSize: Int)  extends Group {

  val textFont = Font("Sans-Serif", FontWeight.ExtraBold, cellSize * 0.3)

  val background = new Rectangle {
    layoutX = cellSize / 2
    layoutY = cellSize / 2

    width = cellSize * 8
    height = cellSize * 8

    fill = SaddleBrown
  }

  children = background

  for {
    row <- 0 until 8
    col <- 0 until 8
    whiteCell = (row+col)%2 != 0
    cellColor = if (whiteCell) LightYellow else SaddleBrown
  } children.add(new Rectangle{
    layoutX = cellSize * (0.5 + col)
    layoutY = cellSize * (0.5 + row)
    width = cellSize
    height = cellSize
    fill = cellColor
  }.delegate)

  for {
    (letter, col) <- "ABCDEFGH".zipWithIndex
  } {
    children.add(new Text(cellSize * (0.9 + col), cellSize * 0.4, s"$letter") {
      font = textFont
    }.delegate)
    children.add(new Text(cellSize * (0.9 + col), cellSize * 8.9, s"$letter") {
      font = textFont
    }.delegate)
  }

  for {
    (digit, line) <- "87654321".zipWithIndex
  } {
    children.add(new Text(cellSize * 0.1, cellSize * (1.2 + line), s"$digit") {
      font = textFont
    }.delegate)
    children.add(new Text(cellSize * 8.6, cellSize * (1.2 + line), s"$digit") {
      font = textFont
    }.delegate)
  }

}
