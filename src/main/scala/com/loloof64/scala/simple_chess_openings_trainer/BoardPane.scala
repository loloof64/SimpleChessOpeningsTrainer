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

import java.awt.{BasicStroke, Color, Component, Cursor, Dimension, Font, Graphics, Graphics2D, Image, Point, Polygon, Toolkit}
import java.awt.event.{MouseAdapter, MouseEvent, MouseMotionAdapter}
import java.awt.geom.AffineTransform
import java.io.{File, FileInputStream, FileNotFoundException, FileOutputStream}
import java.util.{Properties, Timer, TimerTask}
import javax.imageio.ImageIO
import javax.swing._
import javax.swing.filechooser.FileNameExtensionFilter

import chesspresso.Chess
import chesspresso.game.Game
import chesspresso.move.{IllegalMoveException, Move}
import chesspresso.pgn.PGNReader

class BoardPane(val cellSize: Int) extends JPanel{

  def loadPgnFile() : Unit = {
    try {
      val file = getFileFromFileChooser
      playerColor = askPlayerColor()
      val stream = new FileInputStream(file)
      relatedGame = new PGNReader(stream, "game").parseGame()
      stream.close()
      relatedGame.gotoStart()

      moveToHighlight = None

      reversed = if (playerColor == Chess.BLACK) true else false
      if (relatedGame.getPosition.getToPlay == playerColor) {
        addListeners()
      } else {
        removeListeners()
        makeComputerPlay()
      }

      repaint()
    } catch {
      case _:CancelledFileChooserException =>
    }
  }

  override def getPreferredSize: Dimension = {
    new Dimension(cellSize*9, cellSize*9)
  }

  override def paintComponent(g: Graphics): Unit = {
    clearComponent(g)
    drawCells(g)
    drawCoords(g)
    drawPlayerTurn(g)
    drawPieces(g)
    drawHighlightedMove(g)
  }

  private def loadPreferences() : Unit = {
    val userHome = new File(System.getProperty("user.home"))
    val preferencesFile = new File(userHome, "SimpleChessOpeningsTrainer.properties")
    val properties = new Properties()

    try {
      val input = new FileInputStream(preferencesFile)
      properties.load(input)

      val currentFolderProp = properties.getProperty(BoardPane.CurrentFolderPropertyKey)
      currentChooserFolder = if (currentFolderProp != null) new File(currentFolderProp) else null
    } catch {
      case _:FileNotFoundException => currentChooserFolder = null
    }

  }

  private def savePreferences() : Unit = {
    val userHome = new File(System.getProperty("user.home"))
    val preferencesFile = new File(userHome, "SimpleChessOpeningsTrainer.properties")
    val properties = new Properties()
    val output = new FileOutputStream(preferencesFile)

    properties.setProperty(BoardPane.CurrentFolderPropertyKey, currentChooserFolder.getAbsolutePath)
    properties.store(output, null)
  }

  private def removeListeners() = {
    removeMouseListener(theMouseListener)
    removeMouseMotionListener(theMouseMotionListener)
  }

  private def addListeners() = {
    addMouseListener(theMouseListener)
    addMouseMotionListener(theMouseMotionListener)
  }

  private def makeComputerPlay() : Unit = {
    if (relatedGame.hasNextMove) {

      val linesMoves = getNextLinesMoves
      val selectedLineIndex = random.nextInt(linesMoves.size)

      val move = linesMoves(selectedLineIndex)
      val fromCell = (move.getFromSqi % 8, move.getFromSqi / 8)
      val toCell = (move.getToSqi % 8, move.getToSqi / 8)
      animatePiece(fromCell, toCell, (startFile, startRank, endFile, endRank) => {
        manageTheAfterComputerMove(startFile, startRank, endFile, endRank, selectedLineIndex)
      })
    }
    else showEndOfVariationDialog()
  }

  private def getNextLinesMoves : List[Move] = {
    var linesMoves = List[Move]()
    var lineIndex = 0
    var loopGoesOn = true
    while (loopGoesOn){
      try {
        val move = relatedGame.getNextMove(lineIndex)
        move.getMovingPiece
        linesMoves = linesMoves :+ move
        lineIndex += 1
      }
      catch {
        case _:NullPointerException => loopGoesOn = false
      }
    }
    linesMoves
  }

  private def showEndOfVariationDialog() : Unit = {
    JOptionPane.showMessageDialog(this, "End of variation")
  }

  private def manageTheAfterComputerMove(oldStartFile: Int, oldStartRank: Int, oldEndFile: Int, oldEndRank: Int, selectedLineIndex: Int): Unit = {
    relatedGame.goForward(selectedLineIndex)
    repaint()

    if (relatedGame.getPosition.getToPlay == playerColor){
      if (relatedGame.hasNextMove) {
        moveToHighlight = Some(oldStartFile, oldStartRank, oldEndFile, oldEndRank)
        repaint()
        addListeners()
      }
      else showEndOfVariationDialog()
    }
    else {
      removeListeners()
      makeComputerPlay()
    }
  }

  private def getFileFromFileChooser : File = {
    val fileChooser = new JFileChooser(currentChooserFolder)
    fileChooser.setAcceptAllFileFilterUsed(false)
    fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PGN file (*.pgn)", "pgn"))
    val dialogResult = fileChooser.showOpenDialog(this)
    if (dialogResult == JFileChooser.APPROVE_OPTION) {
      currentChooserFolder = fileChooser.getCurrentDirectory
      savePreferences()
      fileChooser.getSelectedFile
    }
    else throw new CancelledFileChooserException
  }

  private def askPlayerColor() : Int = {
    val dialog = new JDialog(SwingUtilities.getWindowAncestor(this).asInstanceOf[JFrame], true)
    dialog.setTitle("Choose your side")
    dialog.setUndecorated(true)

    var result: Int = Int.MinValue

    val buttons = new JPanel()
    buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS))
    val whiteButton = new JButton("White")
    whiteButton.addActionListener{(event) => result = Chess.WHITE; dialog.setVisible(false)}
    val blackButton = new JButton("Black")
    blackButton.addActionListener{(event) => result = Chess.BLACK; dialog.setVisible(false)}
    val nobodyButton = new JButton("Nobody")
    nobodyButton.addActionListener{(event) => result = Chess.NOBODY; dialog.setVisible(false)}
    buttons.add(whiteButton)
    buttons.add(blackButton)
    buttons.add(nobodyButton)

    val contents = new JPanel()
    contents.setLayout(new BoxLayout(contents, BoxLayout.PAGE_AXIS))
    val label = new JLabel("Choose your side")
    label.setAlignmentX(Component.CENTER_ALIGNMENT)
    contents.add(label)
    contents.add(buttons)

    dialog.getContentPane.add(contents)
    dialog.pack()

    dialog.setLocationRelativeTo(null)
    dialog.setVisible(true)

    result
  }

  private def clearComponent(g: Graphics): Unit = {
    g.setColor(new Color(0xff9999))
    g.fillRect(0, 0, getWidth, getHeight)
  }

  private def drawCells(g: Graphics): Unit = {
    for {
      rank <- 0 until 8
      file <- 0 until 8
      isWhite = (rank+file)%2 != 0
      color = if (isWhite) 0xffff66 else 0x4d1a00
      highlightColor = 0x009900
      absCoords = cellCoordsToAbsoluteCoords((file, rank))
    } {
      g.setColor(new Color(dragStartCoord match {
        case Some(startCell) => if (startCell == (file, rank)) highlightColor else color
        case None => color
      }))
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
    def drawAPiece(file:Int, rank:Int, piece: Int) = {
      val draw = (abs:Int, ord:Int) => g.drawImage(ImageIO.read(getClass.getResourceAsStream(pieceToPictureRef(piece))),
        abs, ord, cellSize, cellSize, null)
      if (piece != Chess.NO_STONE) {
        val (abs, ord) = cellCoordsToAbsoluteCoords((file, rank))
        dragStartCoord match {
          case Some(startCell) => if (startCell != (file, rank)) draw(abs, ord)
          case None => draw(abs, ord)
        }
      }
    }

    for {
      rank <- 0 until 8
      file <- 0 until 8
      piece = relatedGame.getPosition.getStone(rank*8 + file)
    } {
      animationPieceStartCell match {
        case Some(startCell) => if (startCell != (file, rank)) drawAPiece(file, rank, piece)
        case None => drawAPiece(file, rank, piece)
      }
    }

    animationPieceLocation match {
      case Some(location) =>
        g.drawImage(ImageIO.read(getClass.getResourceAsStream(pieceToPictureRef(animationPiece.get))),
          location._1, location._2, cellSize, cellSize, null)
      case None =>
    }
  }

  private def drawHighlightedMove(g: Graphics): Unit = {
    moveToHighlight match {
      case Some((startFile, startRank, endFile, endRank)) =>
        val (absoluteStart) = cellCoordsToAbsoluteCoords((startFile, startRank))
        val (absoluteEnd) = cellCoordsToAbsoluteCoords((endFile, endRank))
        val absStart = (absoluteStart._1 + cellSize * 0.5).toInt
        val ordStart = (absoluteStart._2 + cellSize * 0.5).toInt
        val absEnd = (absoluteEnd._1 + cellSize * 0.5).toInt
        val ordEnd = (absoluteEnd._2 + cellSize * 0.5).toInt
        val oldStroke = g.asInstanceOf[Graphics2D].getStroke
        g.asInstanceOf[Graphics2D].setStroke(new BasicStroke(5))
        g.setColor(Color.BLUE)
        g.drawLine(absStart, ordStart, absEnd, ordEnd)

        val arrowHead = new Polygon()
        val len = (cellSize * 0.2).toInt
        arrowHead.addPoint(-len, 0)
        arrowHead.addPoint(len, 0)
        arrowHead.addPoint(0, len)

        val tx = new AffineTransform()
        tx.setToIdentity()
        val angle = -Math.atan2(absEnd - absStart, ordEnd - ordStart)
        tx.translate(absEnd, ordEnd)
        tx.rotate(angle)


        val oldTransform = g.asInstanceOf[Graphics2D].getTransform
        g.asInstanceOf[Graphics2D].setTransform(tx)
        g.fillPolygon(arrowHead)
        g.asInstanceOf[Graphics2D].setTransform(oldTransform)

        g.asInstanceOf[Graphics2D].setStroke(oldStroke)
      case None =>
    }
  }

  private def cellCoordsToAbsoluteCoords(cell: (Int, Int)) : (Int, Int) = {
    if (reversed) ((cellSize * (7.5-cell._1)).toInt, (cellSize * (0.5+cell._2)).toInt)
    else ((cellSize * (0.5+cell._1)).toInt, (cellSize * (7.5-cell._2)).toInt)
  }

  private def absoluteCoordsToCellCoords(absCoords: (Int, Int)) : (Int, Int) = {
    if (reversed) (7 - ((absCoords._1 - cellSize*0.5)/cellSize).toInt, ((absCoords._2 - cellSize*0.5)/cellSize).toInt)
    else (((absCoords._1 - cellSize*0.5)/cellSize).toInt, 7 - ((absCoords._2 - cellSize*0.5)/cellSize).toInt)
  }

  private def validatePromotion(piece: Short) = {
    pendingPromotionInfo match {
      case Some((sqiFrom, sqiTo, isCapturing)) =>
        pendingPromotionInfo = None
        val startCell = (sqiFrom%8, sqiFrom/8)
        val endCell = (sqiTo%8, sqiTo/8)
        if (relatedGame.hasNextMove) {
          giveAnswerToPlayerMove(startCell, endCell, Move.getPawnMove(sqiFrom, sqiTo, isCapturing, piece))
        }
      case None =>
    }
  }

  private def askForPromotionMove(toPlay: Int) = {
    val dialog = new JDialog(SwingUtilities.getWindowAncestor(this).asInstanceOf[JFrame], true)
    dialog.setTitle("Promotion piece")

    val queenImage = ImageIO.read(getClass.getResourceAsStream(pieceToPictureRef(if (toPlay == Chess.BLACK) Chess.BLACK_QUEEN else Chess.WHITE_QUEEN)))
    val rookImage = ImageIO.read(getClass.getResourceAsStream(pieceToPictureRef(if (toPlay == Chess.BLACK) Chess.BLACK_ROOK else Chess.WHITE_ROOK)))
    val bishopImage = ImageIO.read(getClass.getResourceAsStream(pieceToPictureRef(if (toPlay == Chess.BLACK) Chess.BLACK_BISHOP else Chess.WHITE_BISHOP)))
    val knightImage = ImageIO.read(getClass.getResourceAsStream(pieceToPictureRef(if (toPlay == Chess.BLACK) Chess.BLACK_KNIGHT else Chess.WHITE_KNIGHT)))

    val queenButton = new JButton(new ImageIcon(queenImage))
    val rookButton = new JButton(new ImageIcon(rookImage))
    val bishopButton = new JButton(new ImageIcon(bishopImage))
    val knightButton = new JButton(new ImageIcon(knightImage))

    queenButton.addActionListener{ (event) => validatePromotion(Chess.QUEEN); dialog.setVisible(false) }
    rookButton.addActionListener { (event) => validatePromotion(Chess.ROOK); dialog.setVisible(false) }
    bishopButton.addActionListener { (event) => validatePromotion(Chess.BISHOP); dialog.setVisible(false) }
    knightButton.addActionListener { (event) => validatePromotion(Chess.KNIGHT); dialog.setVisible(false) }

    val buttons = new JPanel()
    buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS))
    buttons.add(queenButton)
    buttons.add(rookButton)
    buttons.add(bishopButton)
    buttons.add(knightButton)

    val components = new JPanel()
    components.setLayout(new BoxLayout(components, BoxLayout.PAGE_AXIS))
    val label = new JLabel("Choose your promotion piece")
    label.setAlignmentX(Component.CENTER_ALIGNMENT)
    components.add(label)
    components.add(buttons)


    dialog.getContentPane.add(components)
    dialog.pack()
    dialog.setLocationRelativeTo(null)

    dialog.setVisible(true)
  }

  private val theMouseListener = new MouseAdapter {

    override def mousePressed(e: MouseEvent): Unit = {
      val (file, rank) = absoluteCoordsToCellCoords((e.getX, e.getY))
      val inBounds = (0 until 8).contains(file) && (0 until 8).contains(rank)
      val piece = relatedGame.getPosition.getStone(file + 8*rank)
      if (inBounds && !dragStarted && piece != Chess.NO_STONE) {
        draggedPiece = Some(piece)
        dragStartCoord = Some((file, rank))

        val toolkit = Toolkit.getDefaultToolkit
        val image = ImageIO.read(getClass.getResourceAsStream(pieceToPictureRef(piece)))
        val cursor = toolkit.createCustomCursor(image.getScaledInstance(32, 32, Image.SCALE_DEFAULT), new Point(0,0), "piece")

        oldCursor = getCursor
        setCursor(cursor)

        dragStarted = true
        repaint()
      }
    }

    override def mouseReleased(e: MouseEvent): Unit = {
      val (file, rank) = absoluteCoordsToCellCoords((e.getX, e.getY))
      val inBounds = (0 until 8).contains(file) && (0 until 8).contains(rank)

      if (inBounds && dragStarted) {
        try {
          val move = validateMove(dragStartCoord.get._1 +8*dragStartCoord.get._2, file + 8*rank)
          val startCell = (dragStartCoord.get._1, dragStartCoord.get._2)
          if (relatedGame.hasNextMove){
            giveAnswerToPlayerMove(startCell, (file, rank), move)
          }
        } catch {
          case _:IllegalMoveException =>
          case _:WaitingForPromotionPieceChooseException => askForPromotionMove(relatedGame.getPosition.getToPlay)
        }
        dragStartCoord = None
        draggedPiece = None
        setCursor(oldCursor)

        dragStarted = false
        repaint()
      }
    }
  }

  private val theMouseMotionListener = new MouseMotionAdapter {
    override def mouseDragged(e: MouseEvent): Unit = {
      repaint()
    }
  }

  private def giveAnswerToPlayerMove(startCell : (Int, Int), endCell : (Int, Int), move: Short) : Unit = {
    removeListeners()
    val nextLinesMoves = getNextLinesMoves.map{_.getShortMoveDesc}
    val isAnExpectedMove = nextLinesMoves.contains(move)
    if (!isAnExpectedMove){
      JOptionPane.showMessageDialog(this, "Wrong move !", "wrong move", JOptionPane.ERROR_MESSAGE)
      val rightMoveLineIndex = random.nextInt(nextLinesMoves.size)
      val rightMove = relatedGame.getNextMove(rightMoveLineIndex)
      val rightMoveStart = rightMove.getFromSqi
      val rightMoveEnd = rightMove.getToSqi
      val startCell = (rightMoveStart%8, rightMoveStart/8)
      val endCell = (rightMoveEnd%8, rightMoveEnd/8)
      animatePiece(startCell, endCell, (_,_,_,_) => {
        moveToHighlight = None
        relatedGame.goForward(rightMoveLineIndex)
        repaint()
        makeComputerPlay()
      })
    }
    else {
      val matchingLine = nextLinesMoves.indexOf(move)
      relatedGame.goForward(matchingLine)
      repaint()
      makeComputerPlay()
    }
  }

  private def animatePiece(startCell : (Int, Int), endCell : (Int, Int), terminationCallback : (Int, Int, Int, Int) => Unit): Unit = {
    if (! animationStarted){
      val fromCoords = cellCoordsToAbsoluteCoords(startCell)
      val toCoords = cellCoordsToAbsoluteCoords(endCell)

      val deltas = (toCoords._1 - fromCoords._1, toCoords._2 - fromCoords._2)
      val timeMs = 800

      val startTime = System.currentTimeMillis()

      val timerTask = new TimerTask {

        override def run(): Unit = {
          val elapsedTime = System.currentTimeMillis() - startTime
          if (elapsedTime < timeMs){
            val percent = elapsedTime * 1.0 / timeMs
            animationPieceLocation = Some((fromCoords._1 + deltas._1 * percent).toInt, (fromCoords._2 + deltas._2 * percent).toInt)
            SwingUtilities.invokeLater{() => repaint()}
          }
          else {
            cancel()

            val oldMoveStartFile = animationPieceStartCell.get._1
            val oldMoveStartRank = animationPieceStartCell.get._2
            val oldMoveEndFile = animationPieceEndCell.get._1
            val oldMoveEndRank = animationPieceEndCell.get._2

            animationPieceLocation = None
            animationPieceStartCell = None
            animationPieceEndCell = None
            animationPiece = None
            animationStarted = false

            terminationCallback(oldMoveStartFile, oldMoveStartRank, oldMoveEndFile, oldMoveEndRank)
          }
        }
      }

      val timer = new Timer
      animationPieceStartCell = Some(startCell)
      animationPieceEndCell = Some(endCell)
      animationPiece = Some(relatedGame.getPosition.getStone(startCell._1 + 8*startCell._2))
      animationStarted = true
      timer.scheduleAtFixedRate(timerTask, 0.toLong, 50)
    }
  }

  private def pieceToPictureRef(piece: Int) : String = {
    piece match {
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
  }

  /**
    *Taken from the ChessCraft project : https://github.com/desht/ChessCraft (under GPLv3)
		*Translated into Scala and slightly modified by loloof64
 *
 * Check if the move is really allowed.  Also account for special cases:
    * castling, en passant, pawn promotion
 *
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

  loadPreferences()

  private var dragStarted = false
  private var draggedPiece : Option[Int] = None
  private var dragStartCoord : Option[(Int, Int)] = None
  private var oldCursor:Cursor = _

  private var animationStarted = false
  private var animationPiece : Option[Int] = None
  private var animationPieceLocation : Option[(Int, Int)] = None
  private var animationPieceStartCell : Option[(Int, Int)] = None
  private var animationPieceEndCell : Option[(Int, Int)] = None

  private var moveToHighlight : Option[(Int, Int, Int, Int)] = None

  private var reversed = false
  private var playerColor = Chess.NOBODY
  private var relatedGame = new Game()
  relatedGame.getPosition.clear()

  private var pendingPromotionInfo : Option[(Int, Int, Boolean)] = None

  private var currentChooserFolder: File = _

  private val random = scala.util.Random
}

object BoardPane {
  val CurrentFolderPropertyKey = "CurrentFolder"
}

class WaitingForPromotionPieceChooseException extends Exception
class NotAPromotionMoveException extends Exception
class CancelledFileChooserException extends  Exception