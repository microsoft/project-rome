//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// This code is licensed under the Microsoft Public License.
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************

using System;
using System.Diagnostics;

namespace SDKTemplate
{
    class Logger
    {
        public static Logger Instance { get; } = new Logger();

        public delegate void LogEventHandler(object sender, string message);
        public event LogEventHandler LogUpdated;

        public string AppLogs
        {
            get; set;
        }

        Logger()
        {
            AppLogs = string.Empty;
        }

        public void LogMessage(string message)
        {
            message = $"[{string.Format("{0:T}", DateTime.Now)}] {message}";
            Debug.WriteLine(message);
            AppLogs = message + Environment.NewLine + AppLogs;
            LogUpdated?.Invoke(this, message);
        }
    }
}
