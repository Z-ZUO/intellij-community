/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package org.zmlx.hg4idea.repo;

import com.intellij.dvcs.repo.RepositoryImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.Hash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.zmlx.hg4idea.HgNameWithHashInfo;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.command.HgTagBranchCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.util.HgUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * @author Nadya Zabrodina
 */

public class HgRepositoryImpl extends RepositoryImpl implements HgRepository {

  @NotNull private final HgRepositoryReader myReader;
  @NotNull private final VirtualFile myHgDir;
  @NotNull private volatile HgRepoInfo myInfo;
  @NotNull private Set<String> myOpenedBranches = Collections.emptySet();

  @NotNull private volatile HgConfig myConfig;
  private boolean myIsFresh = true;


  @SuppressWarnings("ConstantConditions")
  private HgRepositoryImpl(@NotNull VirtualFile rootDir, @NotNull HgVcs vcs,
                           @NotNull Disposable parentDisposable) {
    super(vcs.getProject(), rootDir, parentDisposable);
    myHgDir = rootDir.findChild(HgUtil.DOT_HG);
    assert myHgDir != null : ".hg directory wasn't found under " + rootDir.getPresentableUrl();
    myReader = new HgRepositoryReader(vcs, VfsUtilCore.virtualToIoFile(myHgDir));
    myConfig = HgConfig.getInstance(getProject(), rootDir);
    update();
  }

  @NotNull
  public static HgRepository getInstance(@NotNull VirtualFile root, @NotNull Project project,
                                         @NotNull Disposable parentDisposable) {
    HgVcs vcs = HgVcs.getInstance(project);
    if (vcs == null) {
      throw new IllegalArgumentException("Vcs not found for project " + project);
    }
    HgRepositoryImpl repository = new HgRepositoryImpl(root, vcs, parentDisposable);
    repository.setupUpdater();
    return repository;
  }

  private void setupUpdater() {
    HgRepositoryUpdater updater = new HgRepositoryUpdater(this);
    Disposer.register(this, updater);
  }

  @NotNull
  @Override
  public VirtualFile getHgDir() {
    return myHgDir;
  }


  @NotNull
  @Override
  public State getState() {
    return myInfo.getState();
  }

  @Override
  @NotNull
  public String getCurrentBranch() {
    return myInfo.getCurrentBranch();
  }

  @Override
  @Nullable
  public String getCurrentRevision() {
    return myInfo.getCurrentRevision();
  }

  @Override
  @NotNull
  public Map<String, Set<Hash>> getBranches() {
    return myInfo.getBranches();
  }

  @Override
  @NotNull
  public Set<String> getOpenedBranches() {
    return myOpenedBranches;
  }

  @NotNull
  @Override
  public Collection<HgNameWithHashInfo> getBookmarks() {
    return myInfo.getBookmarks();
  }

  @Nullable
  @Override
  public String getCurrentBookmark() {
    return myInfo.getCurrentBookmark();
  }

  @NotNull
  @Override
  public Collection<HgNameWithHashInfo> getTags() {
    return myInfo.getTags();
  }

  @NotNull
  @Override
  public Collection<HgNameWithHashInfo> getLocalTags() {
    return myInfo.getLocalTags();
  }

  @NotNull
  @Override
  public HgConfig getRepositoryConfig() {
    return myConfig;
  }

  @Override
  public boolean isFresh() {
    return myIsFresh;
  }

  @Override
  public void update() {
    HgRepoInfo currentInfo = readRepoInfo();
    // update only if something changed!!!   if update every time - new log will be refreshed every time, too.
    // Then blinking and do not work properly;
    if (!Disposer.isDisposed(getProject()) && !currentInfo.equals(myInfo)) {
      myInfo = currentInfo;
      myOpenedBranches = updateOpenedBranches();
      getProject().getMessageBus().syncPublisher(HgVcs.STATUS_TOPIC).update(getProject(), getRoot());
    }
  }

  @NotNull
  private Set<String> updateOpenedBranches() {
    HgCommandResult branchCommandResult = new HgTagBranchCommand(getProject(), getRoot()).collectBranches();
    return HgTagBranchCommand.collectNames(branchCommandResult);
  }

  @NotNull
  @Override
  public String toLogString() {
    return String.format("HgRepository " + getRoot() + " : " + myInfo);
  }

  @NotNull
  private HgRepoInfo readRepoInfo() {
    myIsFresh = myIsFresh && myReader.isFresh();
    //in GitRepositoryImpl there are temporary state object for reader fields storing! Todo Check;
    return
      new HgRepoInfo(myReader.readCurrentBranch(), myReader.readCurrentRevision(), myReader.readState(), myReader.readBranches(),
                     myReader.readBookmarks(), myReader.readCurrentBookmark(), myReader.readTags(), myReader.readLocalTags());
  }

  public void updateConfig() {
    myConfig = HgConfig.getInstance(getProject(), getRoot());
  }
}
