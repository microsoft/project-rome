//-------------------------------------------------------------------------
//  <copyright file="RouteConfig.cs" company="Microsoft">
//      Copyright (c) Microsoft Corporation.  All rights reserved.
//  </copyright>
//  <project>GraphNotificationsUsingUnsId</project>
//  <summary>
//      Class in which all the routes are defined
//  </summary>
//-------------------------------------------------------------------------

using System.Web.Http;
using System.Web.Routing;

namespace Microsoft.Graph.Notifications.Sample.GraphNotificationsUsingUnsId.App_Start
{
    /// <summary>
    /// Class in which all the routes are defined
    /// </summary>
    public class RouteConfig
    {
        public static void RegisterRoutes(RouteCollection routes)
        {
            routes.Ignore("{resource}.axd/{*pathInfo}");

            routes.MapHttpRoute(
                name: "UnsId",
                routeTemplate: "unsid",
                defaults: new { controller = "UnsId", action = "PostAsync" }
            );

            routes.MapHttpRoute(
                name: "Default",
                routeTemplate: "{controller}/{action}/{id}",
                defaults: new { controller = "Home", action = "IndexAsync", id = RouteParameter.Optional }
            );
        }
    }
}