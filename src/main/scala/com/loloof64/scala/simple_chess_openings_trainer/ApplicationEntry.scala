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

import java.awt.event.KeyEvent
import javax.swing.{JFrame, JMenu, JMenuBar, JMenuItem}

object ApplicationEntry extends App {
  val frame = new JFrame("Simple chess openings trainer")
  val boardPane = new BoardPane(50)
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  frame.getContentPane.add(boardPane)

  val menuBar = new JMenuBar
  val fileMenu = new JMenu("File")
  fileMenu.setMnemonic('F')
  val loadFileMenuItem = new JMenuItem("Load", KeyEvent.VK_L)
  loadFileMenuItem.addActionListener { (event) => boardPane.loadPgnFile()}
  fileMenu.add(loadFileMenuItem)
  menuBar.add(fileMenu)

  frame.setJMenuBar(menuBar)

  frame.pack()
  frame.setLocationRelativeTo(null)

  frame.setVisible(true)
}
