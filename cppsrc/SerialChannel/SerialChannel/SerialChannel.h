/*
 * Copyright (C) 2011 Timo Vesalainen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
// The following ifdef block is the standard way of creating macros which make exporting 
// from a DLL simpler. All files within this DLL are compiled with the SERIALCHANNEL_EXPORTS
// symbol defined on the command line. this symbol should not be defined on any project
// that uses this DLL. This way any other project whose source files include this file see 
// SERIALCHANNEL_API functions as being imported from a DLL, whereas this DLL sees symbols
// defined with this macro as being exported.
#ifdef SERIALCHANNEL_EXPORTS
#define SERIALCHANNEL_API __declspec(dllexport)
#else
#define SERIALCHANNEL_API __declspec(dllimport)
#endif

#include "org_vesalainen_comm_channel_winx_WinSerialChannel.h"

void exception(JNIEnv * env, const char* clazz, const char* message);
char* configure(
	JNIEnv *env, 
	HANDLE handle, 
	int bauds, 
	int parity, 
	int databits, 
	int stopbits, 
	int flow,
    DWORD readIntervalTimeout,
    DWORD readTotalTimeoutMultiplier,
    DWORD readTotalTimeoutConstant,
    DWORD writeTotalTimeoutMultiplier,
    DWORD writeTotalTimeoutConstant
	);
