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
package com.loloof64.scala.simple_chess_openings_trainer

import java.awt.{Color, Dimension, Font, Graphics}
import javax.imageio.ImageIO
import javax.swing.JPanel

import chesspresso.Chess
import chesspresso.game.Game

class BoardPane(val cellSize: Int) extends JPanel{

  override def getPreferredSize: Dimension = {
    new Dimension(cellSize*9, cellSize*9)
  }

  override def paintComponent(g: Graphics): Unit = {
    clearComponent(g)
    drawCells(g)
    drawCoords(g)
    drawPlayerTurn(g)
    drawPieces(g)
  }

  private def clearComponent(g: Graphics): Unit = {
    g.setColor(new Color(0xff9999))
    g.fillRect(0, 0, getWidth, getHeight)
  }

  private def drawCells(g: Graphics): Unit = {
    for {
      row <- 0 until 8
      col <- 0 until 8
      isWhite = (row+col)%2 != 0
      color = if (isWhite) 0xffff66 else 0x4d1a00
      absCoords = cellCoordsToAbsoluteCoords((col, row))
    } {
      g.setColor(new Color(color))
      g.fillRect(absCoords._1, absCoords._2, cellSize, cellSize)
    }
  }

  private def drawCoords(g: Graphics): Unit = {
    val letters = if (reversed) "ABCDEFGH".reverse else "ABCDEFGH"
    val digits = if (reversed) "12345678" else "12345678".reverse
    val fontSize = (cellSize * 0.3).toInt

    g.setFont(new Font(g.getFont.getName, Font.BOLD, fontSize))
    g.setColor(Color.BLACK)
    for {
      (theLetter, col) <- letters.zipWithIndex
    } {
      g.drawString(s"$theLetter", (cellSize * (0.9 + col)).toInt, (cellSize * 0.4).toInt)
      g.drawString(s"$theLetter", (cellSize * (0.9 + col)).toInt, (cellSize * 8.9).toInt)
    }

    for {
      (theDigit, row) <- digits.zipWithIndex
    } {
      g.drawString(s"$theDigit", (cellSize * 0.1).toInt, (cellSize * (1.2 + row)).toInt)
      g.drawString(s"$theDigit", (cellSize * 8.6).toInt, (cellSize * (1.2 + row)).toInt)
    }
  }

  private def drawPlayerTurn(g: Graphics): Unit = {
    val isBlack = relatedGame.getPosition.getToPlay == Chess.BLACK
    val color = if (isBlack) Color.BLACK else Color.WHITE

    g.setColor(color)
    g.fillRect((cellSize * 8.5).toInt, (cellSize * 8.5).toInt, cellSize/2, cellSize/2)
  }

  private def drawPieces(g: Graphics): Unit = {
    def drawAPiece(abs:Int, ord:Int, piece: Int) = {
      val pictureRef = piece match {
        case Chess.WHITE_PAWN => "/chess_pl.png"
        case Chess.WHITE_KNIGHT => "/chess_nl.png"
        case Chess.WHITE_BISHOP => "/chess_bl.png"
        case Chess.WHITE_ROOK => "/chess_rl.png"
        case Chess.WHITE_QUEEN => "/chess_ql.png"
        case Chess.WHITE_KING => "/chess_kl.png"

        case Chess.BLACK_PAWN => "/chess_pd.png"
        case Chess.BLACK_KNIGHT => "/chess_nd.png"
        case Chess.BLACK_BISHOP => "/chess_bd.png"
        case Chess.BLACK_ROOK => "/chess_rd.png"
        case Chess.BLACK_QUEEN => "/chess_qd.png"
        case Chess.BLACK_KING => "/chess_kd.png"
      }
      g.drawImage(ImageIO.read(getClass.getResourceAsStream(pictureRef)), abs, ord, cellSize, cellSize, null)
    }

    for {
      row <- 0 until 8
      col <- 0 until 8
      piece = relatedGame.getPosition.getStone(row*8 + col)
    } if (piece != Chess.NO_PIECE) {
      val (abs, ord) = cellCoordsToAbsoluteCoords((col, row))
      drawAPiece(abs, ord, piece)
    }
  }

  private def cellCoordsToAbsoluteCoords(cell: (Int, Int)) : (Int, Int) = {
    if (reversed) ((cellSize * (7.5-cell._1)).toInt, (cellSize * (0.5+cell._2)).toInt)
    else ((cellSize * (0.5+cell._1)).toInt, (cellSize * (7.5-cell._2)).toInt)
  }

  private var reversed = false
  private var relatedGame = new Game()

}
