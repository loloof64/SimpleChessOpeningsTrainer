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

import chesspresso.Chess
import chesspresso.game.Game

import scalafx.scene.Group
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.shape.Rectangle
import scalafx.scene.paint.Color._
import scalafx.scene.text.{Font, FontWeight, Text}

class BoardGroup(cellSize: Int)  extends Group {

  private var relatedGame = new Game

  private val reversed = false

  private val textFont = Font("Sans-Serif", FontWeight.ExtraBold, cellSize * 0.3)

  private def addBackground() = {
    val background = new Rectangle {
      layoutX = cellSize / 2
      layoutY = cellSize / 2

      width = cellSize * 8
      height = cellSize * 8

      fill = SaddleBrown
    }
    children.add(background)
  }

  private def addCells() = {
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
  }

  private def addCoords() = {
    val letters = if (reversed) "ABCDEFGH".reverse else "ABCDEFGH"
    for {
      (letter, col) <- letters.zipWithIndex
    } {
      children.add(new Text(cellSize * (0.9 + col), cellSize * 0.4, s"$letter") {
        font = textFont
      }.delegate)
      children.add(new Text(cellSize * (0.9 + col), cellSize * 8.9, s"$letter") {
        font = textFont
      }.delegate)
    }

    val numbers = if (reversed) "12345678" else "12345678".reverse
    for {
      (digit, line) <- numbers.zipWithIndex
    } {
      children.add(new Text(cellSize * 0.1, cellSize * (1.2 + line), s"$digit") {
        font = textFont
      }.delegate)
      children.add(new Text(cellSize * 8.6, cellSize * (1.2 + line), s"$digit") {
        font = textFont
      }.delegate)
    }
  }

  private def addPieces() = {
    for {
      row <- 0 until 8
      col <- 0 until 8
      piece = relatedGame.getPosition.getStone(row*8 + col)
    } if (piece != Chess.NO_PIECE) {
      val abs = cellSize * (if (reversed) 7.5-col else 0.5 + col)
      val ord = cellSize * (if (reversed) 0.5+row else 7.5 - row)
      val pictureRef = piece match {
        case Chess.WHITE_PAWN => "chess_pl.png"
        case Chess.WHITE_KNIGHT => "chess_nl.png"
        case Chess.WHITE_BISHOP => "chess_bl.png"
        case Chess.WHITE_ROOK => "chess_rl.png"
        case Chess.WHITE_QUEEN => "chess_ql.png"
        case Chess.WHITE_KING => "chess_kl.png"

        case Chess.BLACK_PAWN => "chess_pd.png"
        case Chess.BLACK_KNIGHT => "chess_nd.png"
        case Chess.BLACK_BISHOP => "chess_bd.png"
        case Chess.BLACK_ROOK => "chess_rd.png"
        case Chess.BLACK_QUEEN => "chess_qd.png"
        case Chess.BLACK_KING => "chess_kd.png"
      }

      val picture = new ImageView(new Image(getClass.getResourceAsStream(pictureRef))){
        layoutX = abs.toInt
        layoutY = ord.toInt
        fitWidth = cellSize
        fitHeight = cellSize
      }
      children.add(picture)
    }
  }

  private def addPlayerTurn() = {
    val isBlack = relatedGame.getPosition.getToPlay == Chess.BLACK
    val color = if (isBlack) Black else White

    val square = new Rectangle {
      layoutX = cellSize * 8.5
      layoutY = cellSize * 8.5
      width = cellSize/2
      height = cellSize/2
      fill = color
    }

    children.add(square.delegate)
  }

  def update() = {
    children.clear()
    addBackground()
    addCells()
    addCoords()
    addPieces()
    addPlayerTurn()
  }

  update()
}
