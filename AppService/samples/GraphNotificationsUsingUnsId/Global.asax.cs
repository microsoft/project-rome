//-------------------------------------------------------------------------
//  <copyright file="WebApiApplication.cs" company="Microsoft">
//      Copyright (c) Microsoft Corporation.  All rights reserved.
//  </copyright>
//  <project>GraphNotificationsUsingUnsId</project>
//  <summary>
//      Starting point of this web application
//  </summary>
//-------------------------------------------------------------------------

using System.IO;
using System.Web;
using System.Web.Hosting;
using System.Web.Http;
using System.Web.Routing;
using Microsoft.Graph.Notifications.Sample.GraphNotificationsUsingUnsId.App_Start;

namespace Microsoft.Graph.Notifications.Sample.GraphNotificationsUsingUnsId
{
    /// <summary>
    /// Starting point of this web application
    /// </summary>
    public class WebApiApplication : HttpApplication
    {
        protected void Application_Start()
        {
            // Set current directory and approot environment variable
            string cwd = HostingEnvironment.ApplicationPhysicalPath;
            Directory.SetCurrentDirectory(cwd);

            GlobalConfiguration.Configure(WebApiConfig.Register);
            RouteConfig.RegisterRoutes(RouteTable.Routes);
        }
    }
}
