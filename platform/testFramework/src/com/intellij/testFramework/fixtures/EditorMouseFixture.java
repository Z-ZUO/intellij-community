/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.testFramework.fixtures;

import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.EditorImpl;

import javax.swing.JComponent;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

@SuppressWarnings("MagicConstant")
public class EditorMouseFixture {
  private final EditorImpl myEditor;
  private int myX;
  private int myY;
  private int myModifiers;
  private int myButton = MouseEvent.NOBUTTON;
  private int myLastId;

  public EditorMouseFixture(EditorImpl editor) {
    myEditor = editor;
  }

  public EditorMouseFixture pressAt(int visualLine, int visualColumn) {
    JComponent component = myEditor.getContentComponent();
    component.dispatchEvent(new MouseEvent(component,
                                           myLastId = MouseEvent.MOUSE_PRESSED,
                                           System.currentTimeMillis(),
                                           getModifiers(),
                                           myX = getX(visualColumn),
                                           myY = getY(visualLine),
                                           1,
                                           false,
                                           myButton));
    return this;
  }

  public EditorMouseFixture release() {
    int oldLastId = myLastId;
    JComponent component = myEditor.getContentComponent();
    component.dispatchEvent(new MouseEvent(component,
                                           myLastId = MouseEvent.MOUSE_RELEASED,
                                           System.currentTimeMillis(),
                                           getModifiers(),
                                           myX,
                                           myY,
                                           1,
                                           false,
                                           myButton));
    if (oldLastId == MouseEvent.MOUSE_PRESSED) {
      component.dispatchEvent(new MouseEvent(component,
                                             myLastId = MouseEvent.MOUSE_CLICKED,
                                             System.currentTimeMillis(),
                                             getModifiers(),
                                             myX,
                                             myY,
                                             1,
                                             false,
                                             myButton));
    }
    return this;
  }

  public EditorMouseFixture clickAt(int visualLine, int visualColumn) {
    return pressAt(visualLine, visualColumn).release();
  }

  public EditorMouseFixture dragTo(int visualLine, int visualColumn) {
    JComponent component = myEditor.getContentComponent();
    component.dispatchEvent(new MouseEvent(component,
                                           myLastId = MouseEvent.MOUSE_DRAGGED,
                                           System.currentTimeMillis(),
                                           getModifiers(),
                                           myX = getX(visualColumn),
                                           myY = getY(visualLine),
                                           1,
                                           false,
                                           myButton));
    return this;
  }

  public EditorMouseFixture alt() {
    myModifiers |= InputEvent.ALT_MASK;
    return this;
  }

  public EditorMouseFixture shift() {
    myModifiers |= InputEvent.SHIFT_MASK;
    return this;
  }

  public EditorMouseFixture middle() {
    myButton = MouseEvent.BUTTON2;
    return this;
  }

  private int getX(int visualColumn) {
    return visualColumn * EditorUtil.getSpaceWidth(Font.PLAIN, myEditor);
  }

  private int getY(int visualLine) {
    return visualLine * myEditor.getLineHeight();
  }

  private int getModifiers() {
    if (myButton == MouseEvent.BUTTON2) {
      return myModifiers | InputEvent.ALT_MASK;
    }
    else {
      return myModifiers;
    }
  }
}
