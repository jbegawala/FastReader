/**
 * boilerpipe
 *
 * Copyright (c) 2009, 2014 Christian Kohlschütter
 *
 * The author licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kohlschutter.boilerpipe.labels;

import com.kohlschutter.boilerpipe.document.TextBlock;

/**
 * Some pre-defined labels which can be used in conjunction with {@link TextBlock#addLabel(String)}
 * and {@link TextBlock#hasLabel(String)}.
 */
public final class DefaultLabels {
  public static final String TITLE = "com.kohlschutter.boilerpipe/TITLE";
  public static final String ARTICLE_METADATA = "com.kohlschutter.boilerpipe/ARTICLE_METADATA";
  public static final String INDICATES_END_OF_TEXT = "com.kohlschutter.boilerpipe/INDICATES_END_OF_TEXT";
  public static final String MIGHT_BE_CONTENT = "com.kohlschutter.boilerpipe/MIGHT_BE_CONTENT";
  public static final String VERY_LIKELY_CONTENT = "com.kohlschutter.boilerpipe/VERY_LIKELY_CONTENT";
  public static final String STRICTLY_NOT_CONTENT = "com.kohlschutter.boilerpipe/STRICTLY_NOT_CONTENT";
  public static final String HR = "com.kohlschutter.boilerpipe/HR";
  public static final String LI = "com.kohlschutter.boilerpipe/LI";

  public static final String HEADING = "com.kohlschutter.boilerpipe/HEADING";
  public static final String H1 = "com.kohlschutter.boilerpipe/H1";
  public static final String H2 = "com.kohlschutter.boilerpipe/H2";
  public static final String H3 = "com.kohlschutter.boilerpipe/H3";

  public static final String MARKUP_PREFIX = "<";

  private DefaultLabels() {
    // not to be instantiated
  }
}
