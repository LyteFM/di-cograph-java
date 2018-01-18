package dicograph.modDecomp;
/**
 *   This source file is part of the program for computing the modular
 *   decomposition of undirected graphs.
 *   Copyright (C) 2010 Marc Tedder
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

/* 
 * The different type of splits possible as a result of the refinement used
 * by our algorithm.  A MIXED split occurs when a node has undergone both a 
 * left and right split. 
 */
enum SplitDirection {
	NONE,LEFT,RIGHT,MIXED;
}
