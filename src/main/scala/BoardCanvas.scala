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



import java.io.FileInputStream
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.input.{DragEvent, MouseEvent}

import chesspresso.Chess
import chesspresso.game.Game
import chesspresso.move.{IllegalMoveException, Move}
import chesspresso.pgn.PGNReader

import scalafx.animation.{AnimationTimer, KeyFrame, KeyValue, Timeline}
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, Button, ButtonType, Label}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.{ClipboardContent, DataFormat, TransferMode}
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.paint.Color._
import scalafx.scene.text.{Font, FontWeight}
import scalafx.stage.{FileChooser, Modality, Stage}
import scalafx.util.Duration

class BoardCanvas(cellSize: Int)  extends Canvas(cellSize * 9, cellSize * 9) {

  private val animXProperty = new SimpleDoubleProperty()
  private val animYProperty = new SimpleDoubleProperty()

  private def makeComputerPlay() = {
    val move = relatedGame.getNextMove
    val fromCell = (move.getFromSqi % 8, move.getFromSqi / 8)
    val toCell = (move.getToSqi % 8, move.getToSqi / 8)

    val fromCoords = ((cellSize * (fromCell._1 + 0.5)).toInt, (cellSize * (7.5 - fromCell._2)).toInt)
    val toCoords = ((cellSize * (toCell._1 + 0.5)).toInt, (cellSize * (7.5 - toCell._2)).toInt)

    val timeline = Timeline(Seq(
      KeyFrame(Duration(0), values = Set(KeyValue(animXProperty, fromCoords._1), KeyValue(animYProperty, fromCoords._2))),
      KeyFrame(Duration(800), values = Set(KeyValue(animXProperty, toCoords._1), KeyValue(animYProperty, toCoords._2)))
    ))

    val timer = AnimationTimer { (now) =>
      update()
    }

    animationStartCell = Some(fromCell)
    animationMovedPiece = Some(relatedGame.getPosition.getStone(fromCell._1 + 8*fromCell._2))

    timeline.setOnFinished { (event) =>
      animationStartCell = None
      animationMovedPiece = None
      timer.stop()
      relatedGame.getPosition.doMove(move)
      update()
    }

    timer.start()
    timeline.play()
  }

  def openFile() : Unit = {
    val fileChooser = new FileChooser {
      title = "Open a pgn"
    }

    val selectedFile = fileChooser.showOpenDialog(this.getScene.getWindow)
    val fileStream = new FileInputStream(selectedFile)

    val pgnReader = new PGNReader(fileStream, "currentFile")
    relatedGame = pgnReader.parseGame()
    fileStream.close()

    val dialog = new Alert(AlertType.Confirmation)
    dialog.setTitle("Player side")
    dialog.setContentText("Choose your side")

    val buttonWhite = new ButtonType("White")
    val buttonBlack = new ButtonType("Black")
    val buttonNobody = new ButtonType("Nobody")

    dialog.getButtonTypes.setAll(buttonWhite, buttonBlack, buttonNobody)
    val result = dialog.showAndWait()

    this.playerColor = result.get match {
      case `buttonWhite` => Chess.WHITE
      case `buttonBlack` => Chess.BLACK
      case `buttonNobody` => Chess.NOBODY
    }

    relatedGame.gotoStart()
    if (relatedGame.getPosition.getToPlay != this.playerColor){
      makeComputerPlay()
    }
  }

  private var playerColor = Chess.NOBODY

  private var relatedGame = new Game

  private val reversed = false

  private var draggingStartCell:Option[(Int, Int)] = None
  private var animationStartCell:Option[(Int, Int)] = None
  private var animationMovedPiece:Option[Int] = None

  private val textFont = Font("Sans-Serif", FontWeight.ExtraBold, cellSize * 0.3)

  private var pendingPromotionInfo : Option[(Int, Int, Boolean)] = None

  private def activateDndCallbacks() = {
    onDragOver = (event: DragEvent) => {
      val cellX = ((event.getSceneX - 0.5 * cellSize) / cellSize).toInt
      val cellY = 7 - ((event.getSceneY - 0.5 * cellSize) / cellSize).toInt
      val eventInBounds = cellX >= 0 && cellX <= 7 && cellY >= 0 && cellY <= 7
      if (eventInBounds && event.getDragboard.hasContent(BoardCanvas.DataFormat)) {
        event.acceptTransferModes(TransferMode.Move)
      }

      event.consume()
    }

    onDragDropped = (event: DragEvent) => {
      val dragBoard = event.getDragboard
      val (startFile, startRank) = draggingStartCell.get
      val cellX = ((event.getSceneX - 0.5 * cellSize) / cellSize).toInt
      val cellY = 7 - ((event.getSceneY - 0.5 * cellSize) / cellSize).toInt
      val eventInBounds = cellX >= 0 && cellX <= 7 && cellY >= 0 && cellY <= 7

      val success = if (eventInBounds) {
        if (dragBoard.hasContent(BoardCanvas.DataFormat)) try {
          val realMove = validateMove(startFile + 8 * startRank, cellX + 8 * cellY)
          relatedGame.getPosition.doMove(realMove)
          true
        } catch {
          case _: IllegalMoveException => if (startFile != cellX || startRank != cellY) {
            reactForIllegalMove()
          }
            false
          case _: WaitingForPromotionPieceChooseException =>
            askForPromotionPiece(relatedGame.getPosition.getToPlay)
            true
        } else false
      } else false

      pendingPromotionInfo = None
      draggingStartCell = None
      event.setDropCompleted(success)
    }

    onDragDone = (event: DragEvent) => {
      update()
      event.consume()
    }

    onDragDetected = (event: MouseEvent) => {
      val cellX = ((event.getSceneX - 0.5 * cellSize) / cellSize).toInt
      val cellY = 7 - ((event.getSceneY - 0.5 * cellSize) / cellSize).toInt
      val eventInBounds = cellX >= 0 && cellX <= 7 && cellY >= 0 && cellY <= 7

      if (eventInBounds) {
        val dragBoard = startDragAndDrop(TransferMode.Move)

        val content = new ClipboardContent()
        val pieceInteger: java.lang.Integer = relatedGame.getPosition.getStone(cellX + 8 * cellY)
        content.put(BoardCanvas.DataFormat, pieceInteger)
        dragBoard.setContent(content)

        draggingStartCell = Some((cellX, cellY))
        update()
      }

      event.consume()
    }
  }

  private def removeDndCallbacks() = {
    onDragOver = (event: DragEvent) => ()
    onDragDropped = (event: DragEvent) => ()
    onDragDone = (event: DragEvent) => ()
    onDragDetected = (event: MouseEvent) => ()
  }


  private def clear() : Unit = {
    val gc = this.getGraphicsContext2D
    gc.setFill(LightPink)
    gc.fillRect(0, 0, 9*cellSize, 9*cellSize)
  }

  private def drawCells() : Unit = {
    val gc = this.getGraphicsContext2D
    for {
      row <- 0 until 8
      col <- 0 until 8
      whiteCell = (row+col)%2 != 0
      cellColor = draggingStartCell match {
        case Some(cell) => if (cell == (col, row)) Green else if (whiteCell) LightYellow else SaddleBrown
        case None => if (whiteCell) LightYellow else SaddleBrown
      }
    } {
      gc.setFill(cellColor)
      gc.fillRect(cellSize*(0.5+col), cellSize*(7.5-row), cellSize, cellSize)
    }
  }

  private def drawCoords() = {
    val gc = this.getGraphicsContext2D
    gc.setFont(textFont)
    gc.setFill(Black)
    val letters = if (reversed) "ABCDEFGH".reverse else "ABCDEFGH"
    for {
      (letter, col) <- letters.zipWithIndex
    } {
      gc.fillText(s"$letter", cellSize * (0.9 + col), cellSize * 0.4)
      gc.fillText(s"$letter", cellSize * (0.9 + col), cellSize * 8.9)
    }

    val numbers = if (reversed) "12345678" else "12345678".reverse
    for {
      (digit, line) <- numbers.zipWithIndex
    } {
      gc.fillText(s"$digit", cellSize * 0.1, cellSize * (1.2 + line))
      gc.fillText(s"$digit", cellSize * 8.6, cellSize * (1.2 + line))
    }
  }

  private def drawPieces() = {

    val gc = this.getGraphicsContext2D

    def drawPiece(abs:Int, ord:Int, piece: Int) = {
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

      gc.drawImage(new Image(getClass.getResourceAsStream(pictureRef)), abs, ord, cellSize, cellSize)
    }

    for {
      row <- 0 until 8
      col <- 0 until 8
      piece = relatedGame.getPosition.getStone(row*8 + col)
    } if (piece != Chess.NO_PIECE) {
      val abs = (cellSize * (if (reversed) 7.5-col else 0.5 + col)).toInt
      val ord = (cellSize * (if (reversed) 0.5+row else 7.5 - row)).toInt
      animationStartCell match {
        case Some(startCell) => if (startCell != (col, row))
          drawPiece(abs, ord, piece)
        case None => drawPiece(abs, ord, piece)
      }
    }

    animationStartCell match {
      case Some(_) =>
        drawPiece(animXProperty.doubleValue().toInt, animYProperty.doubleValue().toInt, animationMovedPiece.get)

      case None =>
    }
  }

  private def drawPlayerTurn() = {
    val gc = this.getGraphicsContext2D
    val isBlack = relatedGame.getPosition.getToPlay == Chess.BLACK
    val color = if (isBlack) Black else White

    gc.setFill(color)
    gc.fillRect(cellSize * 8.5, cellSize * 8.5, cellSize/2, cellSize/2)
  }

  def update() = {
    clear()
    drawCells()
    drawCoords()
    drawPieces()
    drawPlayerTurn()
  }

  /**
  Taken from the ChessCraft project : https://github.com/desht/ChessCraft (under GPLv3)
		Translated into Scala and slightly modified by loloof64

    * Check if the move is really allowed.  Also account for special cases:
    * castling, en passant, pawn promotion
    * @param sqiFrom - Int - from square index
    * @param sqiTo - Int - to square index
    * @return Short - move, if allowed
    * @throws IllegalMoveException if not allowed
    * @throws WaitingForPromotionPieceChooseException if user must choose promotion piece before
    */
  private def validateMove(sqiFrom: Int, sqiTo: Int) : Short = {
    import scala.collection.BitSet

    val relatedPosition = relatedGame.getPosition

    val toPlay = relatedPosition.getToPlay
    val availableShortCastle = (BitSet(relatedPosition.getCastles) & (BitSet(Move.WHITE_SHORT_CASTLE) | BitSet(Move.BLACK_SHORT_CASTLE))).isEmpty
    val availableLongCastle = (BitSet(relatedPosition.getCastles) & (BitSet(Move.WHITE_LONG_CASTLE) | BitSet(Move.BLACK_LONG_CASTLE))).isEmpty
    val isCapturing = relatedPosition.getPiece(sqiTo) != Chess.NO_PIECE

    val newMove: Short =
      if (relatedPosition.getPiece(sqiFrom) == Chess.KING) {
        // Castling?
        if (availableShortCastle && (sqiFrom == Chess.E1 && sqiTo == Chess.G1 || sqiFrom == Chess.E8 && sqiTo == Chess.G8)) {
          Move.getShortCastle(toPlay)
        }
        else if (availableLongCastle && (sqiFrom == Chess.E1 && sqiTo == Chess.C1 || sqiFrom == Chess.E8 && sqiTo == Chess.C8)) {
          Move.getLongCastle(toPlay)
        }
        else Move.getRegularMove(sqiFrom, sqiTo, isCapturing)
      }
      else if (relatedPosition.getPiece(sqiFrom) == Chess.PAWN
        && (Chess.sqiToRow(sqiTo) == 7 || Chess.sqiToRow(sqiTo) == 0)) {
        // Promotion?

        pendingPromotionInfo = Some((sqiFrom, sqiTo, isCapturing))
        throw new WaitingForPromotionPieceChooseException
      }
      else if (relatedPosition.getPiece(sqiFrom) == Chess.PAWN && relatedPosition.getPiece(sqiTo) == Chess.NO_PIECE) {
        // En passant?
        val toCol = Chess.sqiToCol(sqiTo)
        val fromCol = Chess.sqiToCol(sqiFrom)
        if ((toCol == fromCol - 1 || toCol == fromCol + 1)
          && (Chess.sqiToRow(sqiFrom) == 4 && Chess.sqiToRow(sqiTo) == 5 || Chess.sqiToRow(sqiFrom) == 3
          && Chess.sqiToRow(sqiTo) == 2)) {
          Move.getEPMove(sqiFrom, sqiTo)
        }
        else Move.getRegularMove(sqiFrom, sqiTo, isCapturing)
      } else Move.getRegularMove(sqiFrom, sqiTo, isCapturing)

    pendingPromotionInfo = None

    if (relatedPosition.getAllMoves.contains(newMove)) newMove
    else throw new IllegalMoveException("")
  }

  private def validatePromotionMove(pieceType : Int) : Unit = {
    pendingPromotionInfo match {
      case Some((sqiFrom, sqiTo, isCapturing)) =>
        val move = Move.getPawnMove(sqiFrom, sqiTo, isCapturing, pieceType)
        relatedGame.getPosition.doMove(move)
        pendingPromotionInfo = None
        update()
      case None =>
    }
  }

  private def reactForIllegalMove() : Unit = {

  }

  private def askForPromotionPiece(toPlay: Int) = {
    val dialog = new Stage
    dialog.initModality(Modality.WindowModal)
    var theResult: Int = Int.MinValue
    dialog.setTitle("Promotion piece")
    dialog.setResizable(false)

    val queenUrl = if (toPlay == Chess.WHITE) "chess_ql.png" else "chess_qd.png"
    val rookUrl = if (toPlay == Chess.WHITE) "chess_rl.png" else "chess_rd.png"
    val bishopUrl = if (toPlay == Chess.WHITE) "chess_bl.png" else "chess_bd.png"
    val knightUrl = if (toPlay == Chess.WHITE) "chess_nl.png" else "chess_nd.png"

    val queenImage = new ImageView(new Image(getClass.getResource(queenUrl).toExternalForm))
    val rookImage = new ImageView(new Image(getClass.getResource(rookUrl).toExternalForm))
    val bishopImage = new ImageView(new Image(getClass.getResource(bishopUrl).toExternalForm))
    val knightImage = new ImageView(new Image(getClass.getResource(knightUrl).toExternalForm))

    val buttonQueen = new Button {
        graphic = queenImage
      onAction = { (event) =>
        theResult = Chess.QUEEN
        dialog.close()
      }
    }
    val buttonRook = new Button {
      graphic = rookImage
      onAction = { (event) =>
        theResult = Chess.ROOK
        dialog.close()
      }
    }
    val buttonBishop = new Button {
      graphic = bishopImage
      onAction = { (event) =>
        theResult = Chess.BISHOP
        dialog.close()
      }
    }
    val buttonKnight = new Button {
      graphic = knightImage
      onAction = { (event) =>
        theResult = Chess.KNIGHT
        dialog.close()
      }
    }

    val hbox = new HBox(buttonQueen, buttonRook, buttonBishop, buttonKnight)
    val label = new Label("Choose your promotion piece")
    val vbox = new VBox(label, hbox)

    dialog.setScene(new Scene(vbox))

    dialog.showAndWait()
    validatePromotionMove(theResult)
  }

  update()
}

object BoardCanvas {
  val DataFormat = new DataFormat("pieceMove")
}

class WaitingForPromotionPieceChooseException extends Exception
class NotAPromotionMoveException extends Exception
