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

import scalafx.application.JFXApp

object ApplicationEntry extends JFXApp {

  val sceneInst = new BoardScene(50)

  stage = new JFXApp.PrimaryStage {
    title.value = "Simple chess openings trainer"
    width = sceneInst.dimension
    height = sceneInst.dimension + 40
    scene = sceneInst
  }

  sceneInst.openFile()

}
