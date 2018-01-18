package dicograph.Editing;

/*
 *   This source file is part of the program for editing directed graphs
 *   into cographs using modular decomposition.
 *   Copyright (C) 2018 Fynn Leitow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

public enum  EditType {
    Lazy,
    BruteForce,
    GreedyILP,
    ILP,
    ILPGlobal,
    None;

    boolean checkPrimesSize(){
        return (this == BruteForce || this == GreedyILP);
    }

    boolean stopAtHard(boolean isFirst){ return isFirst && (this == BruteForce || this == GreedyILP);}

    boolean doLazyOnFirst(){
        return (this == Lazy || this == BruteForce || this == GreedyILP );
    }

    boolean doLazyOnSecond(boolean useGlobalScore){
        return (this == Lazy || useGlobalScore && this == BruteForce);
    }
}
