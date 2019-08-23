//-------------------------------------------------------------------------
//  <copyright file="WebApiConfig.cs" company="Microsoft">
//      Copyright (c) Microsoft Corporation.  All rights reserved.
//  </copyright>
//  <project>GraphNotificationsUsingUnsId</project>
//  <summary>
//      Class in which all the web API routes, configurations and services are defined
//  </summary>
//-------------------------------------------------------------------------
using System.Web.Http;

namespace Microsoft.Graph.Notifications.Sample.GraphNotificationsUsingUnsId.App_Start
{
    /// <summary>
    /// Class in which all the web API routes, configurations and services are defined
    /// </summary>
    public static class WebApiConfig
    {
        /// <summary>
        /// Register the Web API routes, configurations and services
        /// </summary>
        /// <param name="config"></param>
        public static void Register(HttpConfiguration config)
        {
            // Web API routes
            config.MapHttpAttributeRoutes();

            config.Routes.MapHttpRoute(
                name: "DefaultApi",
                routeTemplate: "api/{controller}/{id}",
                defaults: new { id = RouteParameter.Optional }
            );
        }
    }
}
